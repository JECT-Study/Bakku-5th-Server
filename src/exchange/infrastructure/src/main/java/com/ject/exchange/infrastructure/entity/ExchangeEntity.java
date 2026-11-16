package com.ject.exchange.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "exchange")
public class ExchangeEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "request_user_id", nullable = false)
    private Long requestUserId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExchangeStatus status;

    @Column(name = "completed_at", nullable = true)
    private Instant completedAt;

    protected ExchangeEntity() {}

    @Builder
    public ExchangeEntity(Long id, Long postId, Long requestUserId, Long ownerId, OfferType offerType, ExchangeStatus status, Instant completedAt) {
        this.id = id;
        this.postId = postId;
        this.requestUserId = requestUserId;
        this.ownerId = ownerId;
        this.offerType = offerType;
        this.status = status;
        this.completedAt = completedAt;
    }

}
