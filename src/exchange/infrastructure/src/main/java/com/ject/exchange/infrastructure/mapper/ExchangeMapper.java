package com.ject.exchange.infrastructure.mapper;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemEntity;

import java.util.List;

public class ExchangeMapper {

    public static ExchangeEntity toEntity(Exchange exchange) {
        return ExchangeEntity.builder()
                .id(exchange.getId())
                .postId(exchange.getPostId())
                .requestUserId(exchange.getRequestUserId())
                .ownerId(exchange.getOwnerId())
                .offerType(exchange.getOfferType())
                .status(exchange.getStatus())
                .completedAt(exchange.getCompletedAt())
                .build();
    }

    public static Exchange toDomain(ExchangeEntity entity, List<OfferedItemEntity> offeredItems) {
        return Exchange.of(
                entity.getId(),
                entity.getPostId(),
                entity.getRequestUserId(),
                entity.getOwnerId(),
                entity.getOfferType(),
                offeredItems.stream().map(OfferedItemMapper::toDomain).toList(),
                entity.getStatus(),
                entity.getCompletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
