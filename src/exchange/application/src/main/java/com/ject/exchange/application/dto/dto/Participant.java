package com.ject.exchange.application.dto.dto;

public record Participant(
        Long userId,
        String nickname,
        Integer completedCount
) {}
