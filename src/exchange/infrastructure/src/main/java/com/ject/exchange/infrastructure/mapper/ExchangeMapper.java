package com.ject.exchange.infrastructure.mapper;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemEntity;

import java.util.List;
import java.util.Map;

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

    /**
     * @param imageKeysByItemId 아이템 ID별 사진 key (순서대로). 없는 아이템은 사진 없음
     */
    public static Exchange toDomain(ExchangeEntity entity, List<OfferedItemEntity> offeredItems,
                                    Map<Long, List<String>> imageKeysByItemId) {
        return Exchange.of(
                entity.getId(),
                entity.getPostId(),
                entity.getRequestUserId(),
                entity.getOwnerId(),
                entity.getOfferType(),
                offeredItems.stream()
                        .map(item -> OfferedItemMapper.toDomain(item, imageKeysByItemId.getOrDefault(item.getId(), List.of())))
                        .toList(),
                entity.getStatus(),
                entity.getCompletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
