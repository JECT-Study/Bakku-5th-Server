package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.domain.exchange.vo.OfferedPostIds;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

@Getter
public class Exchange {

    private final Long id;
    private final Long postId;
    private final Long requestUserId;
    private final Long ownerId;
    private final OfferType offerType;
    private final List<OfferedItem> offeredItems;
    private final ExchangeStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant completedAt;

    private Exchange(Long id, Long postId, Long requestUserId, Long ownerId, OfferType offerType, List<OfferedItem> offeredItems, ExchangeStatus status, Instant completedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.postId = postId;
        this.requestUserId = requestUserId;
        this.ownerId = ownerId;
        this.offerType = offerType;
        this.offeredItems = offeredItems;
        this.status = status;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     *  기존 교환글을 연결한 교환요청
     * @param postId
     * @param requestUserId
     * @param ownerId
     * @param offeredPostIds
     * @return
     */
    public static Exchange createWithPosts(Long postId, Long requestUserId, Long ownerId, OfferedPostIds offeredPostIds) {
        validateNotOwnPost(requestUserId, ownerId);
        List<Long> postIds = offeredPostIds.values();
        List<OfferedItem> items = IntStream.range(0, postIds.size())
                .mapToObj(i -> OfferedItem.ofPost(postIds.get(i), i))
                .toList();

        return new Exchange(null, postId, requestUserId, ownerId, OfferType.POST, items,
                ExchangeStatus.ACTIVE, null, null, null);
    }

    /**
     * 새로운 아이템을 첨부한 교환요청
     * @param postId
     * @param requesterId
     * @param ownerId
     * @param name
     * @param opened
     * @param description
     * @return
     */
    public static Exchange createWithNewItem(Long postId, Long requesterId, Long ownerId,
                                             ItemName name, Boolean opened, ItemDescription description) {
        validateNotOwnPost(requesterId, ownerId);
        List<OfferedItem> items = List.of(OfferedItem.ofNew(name, opened, description, 0));
        return new Exchange(null, postId, requesterId, ownerId, OfferType.NEW, items, ExchangeStatus.ACTIVE, null, null, null);
    }

    /**
     * 불러온 도메인 데이터 객체화
     * @param postId
     * @param requestUserId
     * @param ownerId
     * @param offerType
     * @param offeredItems
     * @param status
     * @param completedAt
     * @param createdAt
     * @param updatedAt
     * @return
     */
    public static Exchange of(Long id, Long postId, Long requestUserId, Long ownerId, OfferType offerType, List<OfferedItem> offeredItems, ExchangeStatus status, Instant completedAt, Instant createdAt, Instant updatedAt) {
        return new Exchange(id, postId, requestUserId, ownerId, offerType, offeredItems, status, completedAt, createdAt, updatedAt);
    }

    public boolean isParticipant(Long userId) {
        return requestUserId.equals(userId) || ownerId.equals(userId);
    }

    /**
     * 참여자 한 명을 받아 상대방 ID를 돌려준다
     */
    public Long counterpartOf(Long participantId) {
        if (requestUserId.equals(participantId)) {
            return ownerId;
        }
        if (ownerId.equals(participantId)) {
            return requestUserId;
        }
        throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
    }

    /**
     * 진행 중인 교환인지 확인한다. 아니면 EXCHANGE_CLOSED
     */
    public void validateActive() {
        if (this.status != ExchangeStatus.ACTIVE) {
            throw new ExchangeException(ExchangeErrorCode.EXCHANGE_CLOSED);
        }
    }

    /**
     * 약속을 제안할 수 있는지 확인하고, 제안을 받을 상대방 ID를 돌려준다
     * 참여자가 아니면 NOT_EXCHANGE_PARTICIPANT, 진행 중인 교환이 아니면 EXCHANGE_CLOSED (이 순서로 판정)
     */
    public Long appointmentReceiverFor(Long proposerId) {
        Long receiverId = counterpartOf(proposerId);
        if (this.status != ExchangeStatus.ACTIVE) {
            throw new ExchangeException(ExchangeErrorCode.EXCHANGE_CLOSED);
        }
        return receiverId;
    }

    /**
     * 교환 완료. 교환 확인에서 한 명이 교환했어요로 답하면 완료된다 (F-01-2)
     * @param now 완료 시각
     */
    public Exchange complete(Instant now) {
        return new Exchange(id, postId, requestUserId, ownerId, offerType, offeredItems,
                ExchangeStatus.COMPLETED, now, createdAt, updatedAt);
    }

    public Exchange updateNewItem(Long requesterId, ItemName name, Boolean opened, ItemDescription description) {
        if (!this.requestUserId.equals(requesterId)) {
            throw new ExchangeException(ExchangeErrorCode.NOT_REQUESTER);
        }
        if (offerType != OfferType.NEW) {
            throw new ExchangeException(ExchangeErrorCode.NOT_NEW_ITEM_OFFER);
        }
        if (this.status != ExchangeStatus.ACTIVE) {
            throw new ExchangeException(ExchangeErrorCode.EXCHANGE_CLOSED);
        }
        // Todo: 확정 약속이 있으면 NEW_ITEM_LOCKED
        OfferedItem item = newItem();
        OfferedItem updatedItem = item.updateNew(name, opened, description);
        return new Exchange(id, postId, requestUserId, ownerId, offerType,
                List.of(updatedItem), status, completedAt, createdAt, updatedAt);
    }

    /**
     * 새로 작성한 아이템의 사진을 바꾼다 (정식 key, 순서대로)
     */
    public Exchange withNewItemImages(List<String> imageKeys) {
        if (offerType != OfferType.NEW) {
            throw new ExchangeException(ExchangeErrorCode.NOT_NEW_ITEM_OFFER);
        }
        return new Exchange(id, postId, requestUserId, ownerId, offerType,
                List.of(newItem().withImageKeys(imageKeys)), status, completedAt, createdAt, updatedAt);
    }

    private static void validateNotOwnPost(Long requesterId, Long ownerId) {
        if (requesterId.equals(ownerId)) {
            throw new ExchangeException(ExchangeErrorCode.OWN_POST_REQUEST);
        }
    }

    private OfferedItem newItem() {
        return offeredItems.get(0);   // NEW 방식은 아이템이 항상 1개
    }
}
