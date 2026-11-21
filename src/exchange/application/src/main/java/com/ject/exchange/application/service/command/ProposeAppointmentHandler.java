package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProposeAppointmentHandler implements CommandHandler<ProposeAppointmentCommand, Long> {

    private final ExchangeRepository exchangeRepository;
    private final AppointmentRepository appointmentRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Long handle(ProposeAppointmentCommand command) {
        // 같은 교환에 동시에 들어온 제안이 진행 중 약속 검사를 함께 통과하지 않도록 교환을 잠근다
        Exchange exchange = exchangeRepository.findByIdForUpdate(command.exchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        Long receiverId = exchange.appointmentReceiverFor(command.proposerId());
        Instant now = serviceTime.now();
        AppointmentTime time = AppointmentTime.of(
                command.timeType(), serviceTime.toOffset(command.scheduledAt()), serviceTime.toOffset(now));
        validateNoAppointmentInProgress(exchange.getId(), now);
        validatePostOpenForAppointment(exchange.getPostId());
        AppointmentItemIds itemIds = selectItems(exchange, command.offeredItemIds());

        // Todo: 차단 관계 확인 (BLOCKED_USER), 채팅방 "교환을 제안해요" 카드 · 상대 알림
        Appointment appointment = Appointment.propose(exchange.getId(), command.proposerId(), receiverId,
                time, command.place(), itemIds, command.reminderOffsetMinutes(), now);
        return appointmentRepository.save(appointment).getId();
    }

    private void validateNoAppointmentInProgress(Long exchangeId, Instant now) {
        boolean inProgress = appointmentRepository.findAllByExchangeId(exchangeId).stream()
                .anyMatch(appointment -> appointment.isInProgressAt(now));
        if (inProgress) {
            throw new ExchangeException(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS);
        }
    }

    private void validatePostOpenForAppointment(Long postId) {
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
     * 교환할 물건을 정한다. 고른 경우 소속 · 중복을 먼저 보고 교환 가능 여부를 본다.
     * 생략한 경우 고를 수 있는 아이템이 1개면 그것을 고르고, 0개면 교환 불가, 2개 이상이면 지정 필요
     */
    private AppointmentItemIds selectItems(Exchange exchange, List<Long> requestedItemIds) {
        Set<Long> availableItemIds = availableItemIdsOf(exchange);
        if (requestedItemIds == null) {
            if (availableItemIds.isEmpty()) {
                throw new ExchangeException(ExchangeErrorCode.ITEM_NOT_AVAILABLE);
            }
            if (availableItemIds.size() > 1) {
                throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
            }
            return new AppointmentItemIds(List.copyOf(availableItemIds));
        }
        AppointmentItemIds itemIds = new AppointmentItemIds(requestedItemIds);
        Set<Long> offeredItemIds = exchange.getOfferedItems().stream()
                .map(OfferedItem::getId)
                .collect(Collectors.toSet());
        if (!offeredItemIds.containsAll(itemIds.values())) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        if (!availableItemIds.containsAll(itemIds.values())) {
            throw new ExchangeException(ExchangeErrorCode.ITEM_NOT_AVAILABLE);
        }
        return itemIds;
    }

    /**
     * 새로 작성한 아이템은 항상 교환 가능하고, 내 교환글 첨부는 원본 글이 교환 가능일 때만 가능하다
     */
    private Set<Long> availableItemIdsOf(Exchange exchange) {
        List<Long> sourcePostIds = exchange.getOfferedItems().stream()
                .map(OfferedItem::getSourcePostId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, PostInfo> sourcePosts = sourcePostIds.isEmpty()
                ? Map.of()
                : postPort.findAllByIds(sourcePostIds).stream()
                        .collect(Collectors.toMap(PostInfo::id, Function.identity(), (first, second) -> first));
        Set<Long> available = new HashSet<>();
        for (OfferedItem item : exchange.getOfferedItems()) {
            if (item.getSourcePostId() == null) {
                available.add(item.getId());
                continue;
            }
            PostInfo sourcePost = sourcePosts.get(item.getSourcePostId());
            if (sourcePost != null && sourcePost.status() == PostInfo.Status.AVAILABLE) {
                available.add(item.getId());
            }
        }
        return available;
    }
}
