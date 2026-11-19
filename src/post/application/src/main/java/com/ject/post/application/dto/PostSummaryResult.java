package com.ject.post.application.dto;

import java.time.OffsetDateTime;

public record PostSummaryResult(
        Long id,
        String thumbnailKey,
        String title,
        String regionName,
        String status,
        boolean nowExchange,
        OffsetDateTime nowExchangeEndsAt,
        String wantedItem,
        boolean isMine,
        OffsetDateTime createdAt
) {
}
