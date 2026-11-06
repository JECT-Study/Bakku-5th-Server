package com.ject.chat.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이 방 알림 끄기 · 켜기")
public record UpdateRoomNotificationRequest(
        @Schema(description = "false 끄기 / true 켜기", example = "false")
        @NotNull
        Boolean enabled
) {
}
