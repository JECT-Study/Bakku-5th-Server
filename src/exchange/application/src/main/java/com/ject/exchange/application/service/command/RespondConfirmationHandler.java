package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.exchange.ExchangeCompleted;
import com.ject.exchange.application.dto.command.RespondConfirmationCommand;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import com.ject.exchange.domain.repository.PostLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RespondConfirmationHandler implements CommandHandler<RespondConfirmationCommand, Long> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostLockRepository postLockRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Long handle(RespondConfirmationCommand command) {
        // 결과가 정해지면 교환글 · 고른 첨부 글 상태가 바뀌므로 다른 방의 수락과 겹치지 않게 글을 먼저 잠그고,
        // 두 사람의 동시 응답은 약속 행 잠금으로 한 줄로 세운다 (application 규약 "동시 변경 보호")
        List<Long> postIds = appointmentRepository.findPostIdsOf(command.appointmentId());
        postLockRepository.lockAll(postIds);
        Appointment appointment = appointmentRepository.findByIdForUpdate(command.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        // Todo: E-3(UpdateNewItemHandler)도 교환을 잠그게 되면 여기서도 findByIdForUpdate로 읽는다 (지금은 E-3이 잠금 없이 통째로 저장해 이쪽만 잠가도 덮어쓰기를 막지 못한다)
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(command.memberId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        // 결과가 정해진 뒤에는 교환이 완료돼도 중복 · 이유 남기기 판정을 먼저 한다 (spec 결정 6)
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            exchange.validateActive();
        }

        Instant now = serviceTime.now();
        Appointment answered = appointment.answerConfirmation(command.memberId(), command.answer(),
                command.notMetReason(), command.notMetDetail(), now);
        applyDecidedResult(appointment.getStatus(), answered, exchange, postIds, now);
        // Todo: 채팅방 결과 카드 · 상대 알림("약속이 취소됐어요") · 같은 글 다른 방 안내, 교환 확인 카드 발송 · 리마인드 · 자동 완료 예약 취소
        return appointmentRepository.save(answered).getId();
    }

    /**
     * 결과가 처음 정해질 때만 교환 · 글 상태를 바꾸고, 완료면 완료 이벤트를 발행한다. 못 만남 뒤 이유 남기기는 결과가 그대로라 아무것도 하지 않는다
     */
    private void applyDecidedResult(AppointmentStatus before, Appointment answered, Exchange exchange,
                                    List<Long> postIds, Instant now) {
        if (before != AppointmentStatus.CONFIRMED) {
            return;
        }
        // post 요청이 실패하면 응답도 남지 않아야 하므로 저장보다 먼저 요청한다 (accept와 같음)
        if (answered.getStatus() == AppointmentStatus.COMPLETED) {
            exchangeRepository.save(exchange.complete(now));
            postPort.complete(postIds);
            // user BC가 커밋 뒤 두 사람의 완료 횟수를 올린다 (F-01-2 동작 3). 롤백되면 처리되지 않는다
            eventPublisher.publishEvent(new ExchangeCompleted(answered.getId(),
                    List.of(answered.getProposerUserId(), answered.getReceiverUserId()), false));
            return;
        }
        postPort.release(postIds);
    }
}
