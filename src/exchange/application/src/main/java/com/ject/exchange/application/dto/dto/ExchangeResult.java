package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    public static ExchangeResult of(Exchange exchange, Map<Long, PostInfo> posts, Map<Long, UserInfo> users) {
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
                            case RESERVED -> ItemAvailability.RESERVED_ELSEWHERE; // Todo: 이 교환의 약속이면 RESERVED_HERE
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
                            item.getItemCondition(),
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
        if (user == null) { // Todo: 탈퇴 회원 표시 정책 정해지면 반영
            return new Participant(userId, null, 0);
        }
        return new Participant(user.id(), user.nickname(), 0); // Todo: reputation BC 연동 전까지 completedCount 0
    }

    private static PostSummary toPostSummary(Long postId, PostInfo post) {
        if (post == null) { // Todo: 삭제된 글 표시 정책 정해지면 반영
            return new PostSummary(postId, null, null, PostInfo.Status.DELETED);
        }
        return new PostSummary(post.id(), post.title(), post.thumbnailUrl(), post.status());
    }
}
