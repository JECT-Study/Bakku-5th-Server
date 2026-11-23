package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.exchange.OfferType;

public record AppointmentItemResult(
        Long offeredItemId,
        OfferType type,
        String name,            // POST는 원본 글 현재 제목, NEW는 작성한 제품명
        String thumbnailKey     // Todo: 이미지 모듈 전까지 NEW는 null
) {}
