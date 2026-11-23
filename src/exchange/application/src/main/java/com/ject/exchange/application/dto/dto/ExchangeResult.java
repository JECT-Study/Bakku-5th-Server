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
        Long chatRoomId,                        // Todo: 채팅방 생성 전까지 null
        AppointmentButton appointmentButton,    // Todo: 약속 구현 전까지 PROPOSE
        Instant createdAt
) {

    /**
     * @param reservedHerePostIds 이 교환의 확정 약속이 고른 첨부 글 ID. 예약중이면 RESERVED_HERE로 보여 준다
     */
    public static ExchangeResult of(Exchange exchange, Map<Long, PostInfo> posts, Map<Long, UserInfo> users,
                                    Set<Long> reservedHerePostIds) {
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
                            false, // Todo: 교환글 요청자만 수정 가능하도록 구현
                            itemPostSummary,
                            item.getName() == null ? null : item.getName().value(),
                            item.getOpened(),
                            item.getDescription() == null ? null : item.getDescription().value(),
                            List.of() // Todo: 이미지 모듈 전까지 빈 리스트
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
                null, // Todo: 채팅방 생성 전까지 null
                AppointmentButton.PROPOSE, // Todo: 약속 구현 전까지 PROPOSE
                exchange.getCreatedAt()
        );
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
