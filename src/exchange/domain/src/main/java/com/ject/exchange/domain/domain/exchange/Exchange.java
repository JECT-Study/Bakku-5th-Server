package com.ject.exchange.domain.domain.exchange;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Exchange {

    private final Long id;
    private final Long postId;
    private final Long requestUserId;
    private final Long ownerId;
    private final OfferType offerType;
    private final ExchangeStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime completedAt;

    private Exchange(Long id, Long postId, Long requestUserId, Long ownerId, OfferType offerType, ExchangeStatus status, LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.postId = postId;
        this.requestUserId = requestUserId;
        this.ownerId = ownerId;
        this.offerType = offerType;
        this.status = status;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 교환 생성, ExchangeStatus는 ACTIVE로 설정됨
     * @param postId
     * @param requestUserId
     * @param ownerId
     * @param offerType
     * @return
     */
    public static Exchange create(Long postId, Long requestUserId, Long ownerId, OfferType offerType) {
        LocalDateTime now = LocalDateTime.now();
        return new Exchange(null, postId, requestUserId, ownerId, offerType, ExchangeStatus.ACTIVE, null, now, now);
    }

}
