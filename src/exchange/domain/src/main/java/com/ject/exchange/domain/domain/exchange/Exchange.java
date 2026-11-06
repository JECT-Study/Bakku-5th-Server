package com.ject.exchange.domain.domain.exchange;

import lombok.Getter;

import java.time.Instant;

@Getter
public class Exchange {

    private final Long id;
    private final Long postId;
    private final Long requestUserId;
    private final Long ownerId;
    private final OfferType offerType;
    private final ExchangeStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant completedAt;

    private Exchange(Long id, Long postId, Long requestUserId, Long ownerId, OfferType offerType, ExchangeStatus status, Instant completedAt, Instant createdAt, Instant updatedAt) {
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
        Instant now = Instant.now();
        return new Exchange(null, postId, requestUserId, ownerId, offerType, ExchangeStatus.ACTIVE, null, now, now);
    }

}
