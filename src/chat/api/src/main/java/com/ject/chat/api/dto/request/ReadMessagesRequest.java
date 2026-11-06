package com.ject.chat.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "읽음 처리")
public record ReadMessagesRequest(
        @Schema(description = "이 방에서 마지막으로 본 메시지 ID", example = "121")
        @NotNull
        Long lastReadMessageId
) {
}
