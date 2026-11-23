package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.application.port.PostStatusRejectedException;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import com.ject.exchange.domain.repository.PostLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AcceptAppointmentHandler implements CommandHandler<AcceptAppointmentCommand, Long> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostLockRepository postLockRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Long handle(AcceptAppointmentCommand command) {
        // 같은 글 · 첨부 글의 확정이 동시에 처리되지 않도록 걸린 글을 먼저 잠그고, 그 뒤에 약속을 처음 읽어 최신 상태로 판단한다 (E-05-1)
        List<Long> postIds = appointmentRepository.findPostIdsOf(command.appointmentId());
        postLockRepository.lockAll(postIds);
        Appointment appointment = appointmentRepository.findByIdForUpdate(command.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(command.accepterId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        appointment.validateReceiver(command.accepterId());
        exchange.validateActive();

        Instant now = serviceTime.now();
        Appointment confirmed = appointment.accept(command.accepterId(), command.reminderOffsetMinutes(), now);
        List<Long> sourcePostIds = postIds.stream()
                .filter(postId -> !postId.equals(exchange.getPostId()))
                .toList();
        // 판정 순서: 교환글(다른 약속에 묶임 → 글 상태) 다음에 고른 첨부 글 (spec 가정 1)
        validatePostAvailable(exchange.getId(), exchange.getPostId());
        validateSourcePostsAvailable(exchange.getId(), sourcePostIds);

        List<Appointment> autoRejected = appointmentRepository
                .findPendingProposalsInvolving(postIds, exchange.getId(), now).stream()
                .map(Appointment::autoReject)
                .toList();
        // 예약중 요청이 실패하면 확정 · 자동 거절도 남지 않아야 하므로 저장보다 먼저 요청한다 (E-04-1 "한 번에 처리")
        reserve(exchange.getPostId(), sourcePostIds);
        // Todo: 채팅방 "약속이 확정됐어요" · 다른 방 "다른 분과 약속이 먼저 확정돼서…" 카드와 알림, 약속 전 알림 · 교환 확인 예약
        autoRejected.forEach(appointmentRepository::save);
        return appointmentRepository.save(confirmed).getId();
    }

    /**
     * 교환글이 수락 가능한지. 다른 약속에 묶였는지는 exchange 데이터로 먼저 본다 (글 잠금 안에서 판정해 같은 글의 동시 확정을 막는다)
     */
    private void validatePostAvailable(Long exchangeId, Long postId) {
        List<Long> postIds = List.of(postId);
        if (!appointmentRepository.findPostIdsBoundBy(postIds, AppointmentStatus.CONFIRMED, exchangeId).isEmpty()) {
            throw new ExchangeException(ExchangeErrorCode.POST_RESERVED_ELSEWHERE);
        }
        if (!appointmentRepository.findPostIdsBoundBy(postIds, AppointmentStatus.COMPLETED, exchangeId).isEmpty()) {
            throw new ExchangeException(ExchangeErrorCode.POST_NOT_REQUESTABLE);
        }
        PostInfo.Status status = postPort.findById(postId)
                .map(PostInfo::status)
                .orElse(PostInfo.Status.DELETED);
        switch (status) {
            case AVAILABLE -> { }
            case RESERVED -> throw new ExchangeException(ExchangeErrorCode.POST_RESERVED_ELSEWHERE);
            case COMPLETED, DELETED -> throw new ExchangeException(ExchangeErrorCode.POST_NOT_REQUESTABLE);
        }
    }

    /**
     * 고른 첨부 글이 모두 수락 가능한지. 다른 약속(확정 · 완료)에 묶였거나 교환 가능 상태가 아니면 ITEM_NOT_AVAILABLE
     */
    private void validateSourcePostsAvailable(Long exchangeId, List<Long> sourcePostIds) {
        if (sourcePostIds.isEmpty()) {
            return;
        }
        boolean bound = !appointmentRepository.findPostIdsBoundBy(sourcePostIds, AppointmentStatus.CONFIRMED, exchangeId).isEmpty()
                || !appointmentRepository.findPostIdsBoundBy(sourcePostIds, AppointmentStatus.COMPLETED, exchangeId).isEmpty();
        if (bound) {
            throw new ExchangeException(ExchangeErrorCode.ITEM_NOT_AVAILABLE);
        }
        Set<Long> availablePostIds = postPort.findAllByIds(sourcePostIds).stream()
                .filter(post -> post.status() == PostInfo.Status.AVAILABLE)
                .map(PostInfo::id)
                .collect(Collectors.toSet());
        if (!availablePostIds.containsAll(sourcePostIds)) {
            throw new ExchangeException(ExchangeErrorCode.ITEM_NOT_AVAILABLE);
        }
    }

    /**
     * 교환글과 고른 첨부 글을 예약중으로 바꾼다. 판정 뒤 그사이 post에서 거부되면 거부된 글로 에러를 고른다 (spec 결정 3)
     */
    private void reserve(Long postId, List<Long> sourcePostIds) {
        try {
            postPort.reserve(reservationTargetsOf(postId, sourcePostIds));
        } catch (PostStatusRejectedException rejected) {
            ExchangeErrorCode errorCode = postId.equals(rejected.getPostId())
                    ? ExchangeErrorCode.POST_NOT_REQUESTABLE
                    : ExchangeErrorCode.ITEM_NOT_AVAILABLE;
            throw new ExchangeException(errorCode);
        }
    }

    private static List<Long> reservationTargetsOf(Long postId, List<Long> sourcePostIds) {
        return Stream.concat(Stream.of(postId), sourcePostIds.stream()).toList();
    }
}
