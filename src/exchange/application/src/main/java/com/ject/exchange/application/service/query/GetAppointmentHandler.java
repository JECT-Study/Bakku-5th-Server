package com.ject.exchange.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAppointmentHandler implements QueryHandler<GetAppointmentQuery, AppointmentResult> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public AppointmentResult handle(GetAppointmentQuery query) {
        Appointment appointment = appointmentRepository.findById(query.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(query.viewerId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        Instant now = serviceTime.now();
        AppointmentStatus displayStatus = appointment.displayStatusAt(now);
        return new AppointmentResult(
                appointment.getId(),
                appointment.getExchangeId(),
                appointment.getProposerUserId(),
                appointment.getReceiverUserId(),
                displayStatus,
                appointment.getTime().type(),
                appointment.getTime().scheduledAt(),
                appointment.getPlace(),
                itemsOf(appointment, exchange),
                displayStatus == AppointmentStatus.PROPOSED ? appointment.getExpiresAt() : null,
                appointment.reminderOffsetMinutesOf(query.viewerId()),
                appointmentRepository.existsPendingProposalOnPost(exchange.getPostId(), exchange.getId(), now),
                appointment.getConfirmedAt(),
                appointment.getCreatedAt()
        );
    }

    private List<AppointmentItemResult> itemsOf(Appointment appointment, Exchange exchange) {
        Map<Long, OfferedItem> offeredItems = exchange.getOfferedItems().stream()
                .collect(Collectors.toMap(OfferedItem::getId, Function.identity()));
        List<OfferedItem> chosenItems = appointment.getItemIds().values().stream()
                .map(offeredItems::get)
                .filter(Objects::nonNull)
                .toList();
        List<Long> sourcePostIds = chosenItems.stream()
                .map(OfferedItem::getSourcePostId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, PostInfo> sourcePosts = sourcePostIds.isEmpty()
                ? Map.of()
                : postPort.findAllByIds(sourcePostIds).stream()
                        .collect(Collectors.toMap(PostInfo::id, Function.identity(), (first, second) -> first));
        return chosenItems.stream()
                .map(item -> toItemResult(item, exchange.getOfferType(), sourcePosts))
                .toList();
    }

    private static AppointmentItemResult toItemResult(OfferedItem item, OfferType offerType, Map<Long, PostInfo> sourcePosts) {
        if (offerType == OfferType.POST) {
            PostInfo sourcePost = sourcePosts.get(item.getSourcePostId());
            // Todo: 삭제된 글 표시 정책 정해지면 반영
            return new AppointmentItemResult(item.getId(), OfferType.POST,
                    sourcePost == null ? null : sourcePost.title(),
                    sourcePost == null ? null : sourcePost.thumbnailUrl());
        }
        // Todo: 이미지 모듈 전까지 새로 작성한 아이템의 대표 사진은 null
        return new AppointmentItemResult(item.getId(), OfferType.NEW,
                item.getName() == null ? null : item.getName().value(), null);
    }
}
