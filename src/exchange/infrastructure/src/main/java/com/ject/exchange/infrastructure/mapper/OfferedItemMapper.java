package com.ject.exchange.infrastructure.mapper;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemImageEntity;

import java.util.List;
import java.util.stream.IntStream;

public class OfferedItemMapper {

    public static List<OfferedItemEntity> toEntities(ExchangeEntity exchangeEntity, Exchange exchange) {
        return exchange.getOfferedItems().stream()
                .map(item -> toEntity(exchangeEntity, exchange.getOfferType(), item))
                .toList();
    }

    public static OfferedItemEntity toEntity(ExchangeEntity exchangeEntity, OfferType offerType, OfferedItem item) {
        return OfferedItemEntity.builder()
                .id(item.getId())
                .exchange(exchangeEntity)
                .offerType(offerType)
                .sourcePostId(item.getSourcePostId())
                .name(item.getName() == null ? null : item.getName().value())
                .opened(item.getOpened())
                .description(item.getDescription() == null ? null : item.getDescription().value())
                .sortOrder(item.getSortOrder())
                .build();
    }

    public static OfferedItem toDomain(OfferedItemEntity entity, List<String> imageKeys) {
        // NEW 아이템만 제품명/설명 값 객체를 가짐. POST 아이템은 null
        boolean isNew = entity.getOfferType() == OfferType.NEW;
        return OfferedItem.of(
                entity.getId(),
                entity.getSourcePostId(),
                isNew ? new ItemName(entity.getName()) : null,
                entity.getOpened(),
                isNew ? new ItemDescription(entity.getDescription()) : null,
                entity.getSortOrder(),
                imageKeys
        );
    }

    /**
     * 아이템 사진은 순서(sort_order 1부터)대로 저장한다
     */
    public static List<OfferedItemImageEntity> toImageEntities(Long offeredItemId, List<String> imageKeys) {
        return IntStream.range(0, imageKeys.size())
                .mapToObj(index -> OfferedItemImageEntity.builder()
                        .offeredItemId(offeredItemId)
                        .imageKey(imageKeys.get(index))
                        .sortOrder(index + 1)
                        .build())
                .toList();
    }
}
