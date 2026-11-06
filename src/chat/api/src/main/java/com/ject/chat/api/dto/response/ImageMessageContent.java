package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "IMAGE 내용")
public record ImageMessageContent(
        @Schema(description = "사진 URL 1~5개", example = "[\"https://{cdn}/chat/5/a.jpg\"]")
        List<String> imageUrls
) implements MessageContent {
}
