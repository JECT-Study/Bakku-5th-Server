package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.exchange.OfferType;

import java.util.List;

public record OfferedItemResult(
        Long id,
        OfferType type,
        ItemAvailability availability,
        boolean editable,
        PostSummary post,
        String name,
        Boolean opened,
        String description,
        List<String> imageUrls     // Todo: 이미지 모듈 전까지 빈 리스트
) {}
