package com.ject.exchange.application.dto.dto;

public record PostSummary(
        Long id,
        String title,
        String thumbnailKey,
        PostInfo.Status status
) {}
