package com.ject.exchange.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "exchange_offered_item_image")
public class OfferedItemImageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offered_item_id", nullable = false)
    private Long offeredItemId;

    @Column(name = "image_key", nullable = false, length = 255)
    private String imageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected OfferedItemImageEntity() {
    }

    @Builder
    public OfferedItemImageEntity(Long id, Long offeredItemId, String imageKey, int sortOrder) {
        this.id = id;
        this.offeredItemId = offeredItemId;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}
