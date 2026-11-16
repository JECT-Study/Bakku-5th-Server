package com.ject.exchange.application.dto.dto;

public record PostSummary(
        Long id,
        String title,
        String thumbnailUrl,
        PostInfo.Status status
) {}
