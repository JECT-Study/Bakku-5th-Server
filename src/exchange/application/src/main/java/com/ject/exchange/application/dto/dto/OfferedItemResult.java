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
        List<String> imageKeys     // NEW면 정식 사진 key(순서대로), POST면 null
) {}
