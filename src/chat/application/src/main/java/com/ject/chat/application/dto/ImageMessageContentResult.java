package com.ject.chat.application.dto;

import java.util.List;

public record ImageMessageContentResult(List<String> imageUrls) implements ChatMessageContentResult {
}
