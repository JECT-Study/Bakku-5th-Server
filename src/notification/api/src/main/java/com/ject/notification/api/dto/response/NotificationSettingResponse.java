package com.ject.notification.api.dto.response;

import com.ject.notification.api.dto.enums.NotificationSettingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "알림 설정 항목 (2차)")
public record NotificationSettingResponse(
        @Schema(description = "알림 설정 항목", example = "CHAT")
        NotificationSettingType type,

        @Schema(description = "켜짐 여부", example = "true")
        boolean enabled,

        @Schema(description = "마지막 변경 일시", nullable = true)
        OffsetDateTime changedAt
) {
}
