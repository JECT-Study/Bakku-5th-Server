package com.ject.chat.application.dto;

public record CardMessageContentResult(ChatMessageCardType cardType, Long targetId)
        implements ChatMessageContentResult {
}
