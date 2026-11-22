package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.ConfirmationReply;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public record AppointmentResult(
        Long id,
        Long exchangeId,
        Long proposerUserId,
        Long receiverUserId,
        AppointmentStatus status,
        AppointmentTimeType timeType,
        Instant scheduledAt,
        Place place,
        List<AppointmentItemResult> items,
        Instant expiresAt,                      // PROPOSED가 아니면 null
        Integer myReminderOffsetMinutes,
        boolean otherActiveProposalsOnPost,
        Instant confirmedAt,
        Instant createdAt,
        ConfirmationSummary confirmation        // 확정 전이거나 취소된 약속이면 null
) {

    /**
     * 교환 확인 정보 없이 만드는 결과 (교환 확인 전 기능의 테스트 호환용)
     */
    public AppointmentResult(Long id, Long exchangeId, Long proposerUserId, Long receiverUserId,
                             AppointmentStatus status, AppointmentTimeType timeType, Instant scheduledAt,
                             Place place, List<AppointmentItemResult> items, Instant expiresAt,
                             Integer myReminderOffsetMinutes, boolean otherActiveProposalsOnPost,
                             Instant confirmedAt, Instant createdAt) {
        this(id, exchangeId, proposerUserId, receiverUserId, status, timeType, scheduledAt, place, items,
                expiresAt, myReminderOffsetMinutes, otherActiveProposalsOnPost, confirmedAt, createdAt, null);
    }

    /**
     * 조회자 기준 약속 결과. 상태는 조회 시점에 보여 줄 상태(만료 시각이 지난 제안은 EXPIRED)
     * @param sourcePosts 약속에서 고른 첨부 글 정보 (ID별). {@link #sourcePostIdsOf}로 조회한다
     * @param otherActiveProposalsOnPost 같은 글의 다른 교환에 만료 전 제안이 있는지
     */
    public static AppointmentResult of(Appointment appointment, Exchange exchange, Map<Long, PostInfo> sourcePosts,
                                       Long viewerId, boolean otherActiveProposalsOnPost, Instant now) {
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
                chosenItemsOf(appointment, exchange).stream()
                        .map(item -> toItemResult(item, exchange.getOfferType(), sourcePosts))
                        .toList(),
                displayStatus == AppointmentStatus.PROPOSED ? appointment.getExpiresAt() : null,
                appointment.reminderOffsetMinutesOf(viewerId),
                otherActiveProposalsOnPost,
                appointment.getConfirmedAt(),
                appointment.getCreatedAt(),
                confirmationOf(appointment, viewerId, now)
        );
    }

    /**
     * 조회자 기준 교환 확인 정보. 확정 · 완료 · 못 만남 약속에만 있다 (확정 전이거나 취소된 약속은 null)
     */
    private static ConfirmationSummary confirmationOf(Appointment appointment, Long viewerId, Instant now) {
        AppointmentStatus status = appointment.getStatus();
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.COMPLETED
                && status != AppointmentStatus.NOT_MET) {
            return null;
        }
        // 카드 발송 스케줄이 없어 열림 시각이 지났으면 질문이 간 것으로 본다 (spec 가정 4)
        Instant opensAt = appointment.confirmationOpensAt();
        Instant askedAt = opensAt != null && !opensAt.isAfter(now) ? opensAt : null;
        AppointmentStatus result = status == AppointmentStatus.CONFIRMED ? null : status;
        // Todo: 3일 무응답 자동 완료(F-03-1)가 생기면 자동 완료 여부 반영
        return new ConfirmationSummary(askedAt, result, false,
                appointment.replyOf(viewerId).map(ConfirmationReply::answer).orElse(null),
                appointment.getResolvedAt());
    }

    /**
     * 약속에서 고른 아이템 중 기존 글을 첨부한 아이템의 글 ID (PostPort 조회용)
     */
    public static List<Long> sourcePostIdsOf(Appointment appointment, Exchange exchange) {
        return chosenItemsOf(appointment, exchange).stream()
                .map(OfferedItem::getSourcePostId)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<OfferedItem> chosenItemsOf(Appointment appointment, Exchange exchange) {
        Map<Long, OfferedItem> offeredItems = exchange.getOfferedItems().stream()
                .collect(Collectors.toMap(OfferedItem::getId, Function.identity()));
        return appointment.getItemIds().values().stream()
                .map(offeredItems::get)
                .filter(Objects::nonNull)
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
