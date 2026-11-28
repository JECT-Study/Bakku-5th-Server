package com.ject.chat.application.dto;

public record TextMessageContentResult(String text, boolean externalTradeDetected)
        implements ChatMessageContentResult {
}
