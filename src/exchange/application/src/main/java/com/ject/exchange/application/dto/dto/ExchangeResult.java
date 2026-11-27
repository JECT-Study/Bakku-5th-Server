package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ExchangeResult(
        Long id,
        ExchangeStatus status,
        PostSummary post,
        Participant requester,
        Participant owner,
        OfferType offerType,
        List<OfferedItemResult> offeredItems,
        Long chatRoomId,
        AppointmentButton appointmentButton,
        Instant createdAt
) {

    /**
     * 조회자 기준 교환 결과. 새로 작성한 제품은 요청자 본인이고 교환이 진행 중이며 확정 약속이 없을 때만 수정할 수 있다 (C-07-2)
     * @param reservedHerePostIds 이 교환의 확정 약속이 고른 첨부 글 ID. 예약중이면 RESERVED_HERE로 보여 준다
     * @param viewerId 조회하는 회원 ID
     * @param appointmentButton {@link AppointmentButton#of}로 고른 약속 버튼
     * @param newItemLocked 이 교환에 확정 약속이 있는지
     * @param chatRoomId chat BC에서 조회한 실제 채팅방 ID. 아직 없으면 null
     */
    public static ExchangeResult of(Exchange exchange, Map<Long, PostInfo> posts, Map<Long, UserInfo> users,
                                    Set<Long> reservedHerePostIds, Long viewerId,
                                    AppointmentButton appointmentButton, boolean newItemLocked, Long chatRoomId) {
        boolean newItemEditable = exchange.getOfferType() == OfferType.NEW
                && exchange.getRequestUserId().equals(viewerId)
                && exchange.getStatus() == ExchangeStatus.ACTIVE
                && !newItemLocked;
        PostInfo postInfo = posts.get(exchange.getPostId());
        PostSummary postSummary = toPostSummary(exchange.getPostId(), postInfo);
        Participant requester = toParticipant(exchange.getRequestUserId(), users.get(exchange.getRequestUserId()));
        Participant owner = toParticipant(exchange.getOwnerId(), users.get(exchange.getOwnerId()));

        List<OfferedItemResult> offeredItems = exchange.getOfferedItems().stream()
                .map(item -> {
                    PostSummary itemPostSummary = null;
                    ItemAvailability availability = ItemAvailability.AVAILABLE;
                    if (exchange.getOfferType() == OfferType.POST) {
                        PostInfo itemPostInfo = posts.get(item.getSourcePostId());
                        itemPostSummary = toPostSummary(item.getSourcePostId(), itemPostInfo);
                        availability = switch (itemPostSummary.status()) {
                            case AVAILABLE -> ItemAvailability.AVAILABLE;
                            case RESERVED -> reservedHerePostIds.contains(item.getSourcePostId())
                                    ? ItemAvailability.RESERVED_HERE
                                    : ItemAvailability.RESERVED_ELSEWHERE;
                            case COMPLETED -> ItemAvailability.COMPLETED;
                            case DELETED -> ItemAvailability.DELETED;
                        };
                    }
                    return new OfferedItemResult(
                            item.getId(),
                            exchange.getOfferType(),
                            availability,
                            newItemEditable,
                            itemPostSummary,
                            item.getName() == null ? null : item.getName().value(),
                            item.getOpened(),
                            item.getDescription() == null ? null : item.getDescription().value(),
                            exchange.getOfferType() == OfferType.NEW ? item.getImageKeys() : null
                    );
                })
                .toList();

        return new ExchangeResult(
                exchange.getId(),
                exchange.getStatus(),
                postSummary,
                requester,
                owner,
                exchange.getOfferType(),
                offeredItems,
                chatRoomId,
                appointmentButton,
                exchange.getCreatedAt()
        );
    }

    /**
     * 조회자 기준 교환 결과. chatRoomId를 모르는 호출부 호환용 (기존 테스트)
     */
    public static ExchangeResult of(Exchange exchange, Map<Long, PostInfo> posts, Map<Long, UserInfo> users,
                                    Set<Long> reservedHerePostIds, Long viewerId,
                                    AppointmentButton appointmentButton, boolean newItemLocked) {
        return of(exchange, posts, users, reservedHerePostIds, viewerId, appointmentButton, newItemLocked, null);
    }

    /**
     * 조회자를 모르는 결과 (약속 버튼 PROPOSE, 수정 불가). 기존 테스트 호환용
     */
    public static ExchangeResult of(Exchange exchange, Map<Long, PostInfo> posts, Map<Long, UserInfo> users,
                                    Set<Long> reservedHerePostIds) {
        return of(exchange, posts, users, reservedHerePostIds, null, AppointmentButton.PROPOSE, false);
    }

    private static Participant toParticipant(Long userId, UserInfo user) {
        if (user == null) { // 회원 정보를 찾지 못하면 ID만 두고 닉네임 · 완료 횟수는 비운다
            return new Participant(userId, null, 0);
        }
        // 탈퇴 회원도 userId는 유지한다 (참여자 판별용). 닉네임은 user BC가 "탈퇴한 사용자"로 준다
        return new Participant(user.id(), user.nickname(), user.completedCount());
    }

    private static PostSummary toPostSummary(Long postId, PostInfo post) {
        // 삭제됐거나 조회되지 않은 글은 제목 · 사진 없이 DELETED로 보여 준다 (화면은 고정 문구만 쓴다, spec 결정 2)
        if (post == null || post.status() == PostInfo.Status.DELETED) {
            return new PostSummary(postId, null, null, PostInfo.Status.DELETED);
        }
        return new PostSummary(post.id(), post.title(), post.thumbnailKey(), post.status());
    }
}
