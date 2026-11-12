package com.ject.exchange.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.exchange.domain.domain.exchange.OfferType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "exchange_offered_item")
public class OfferedItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private ExchangeEntity exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OfferType offerType;

    @Column(name = "source_post_id")
    private Long sourcePostId;

    @Column(name = "name", length = 40)
    private String name;

    @Column(name = "is_opened")
    private Boolean opened;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected OfferedItemEntity() {}

    @Builder
    public OfferedItemEntity(Long id, ExchangeEntity exchange, OfferType offerType, Long sourcePostId, String name, Boolean opened, String description, int sortOrder) {
        this.id = id;
        this.exchange = exchange;
        this.offerType = offerType;
        this.sourcePostId = sourcePostId;
        this.name = name;
        this.opened = opened;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
