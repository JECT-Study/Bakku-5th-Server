package com.ject.exchange.infrastructure.mapper;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.infrastructure.entity.ExchangeEntity;

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

    public static Exchange toDomain(ExchangeEntity entity) {
        return Exchange.create(
                entity.getPostId(),
                entity.getRequestUserId(),
                entity.getOwnerId(),
                entity.getOfferType()
        );
    }
}
