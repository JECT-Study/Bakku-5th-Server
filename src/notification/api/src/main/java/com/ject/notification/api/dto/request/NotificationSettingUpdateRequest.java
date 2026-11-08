package com.ject.notification.api.dto.request;

import com.ject.notification.api.dto.enums.NotificationSettingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "알림 설정 변경 (2차)")
public record NotificationSettingUpdateRequest(
        @Schema(description = "알림 설정 항목", example = "APPOINTMENT")
        @NotNull
        NotificationSettingType type,

        @Schema(description = "켜기 / 끄기", example = "false")
        @NotNull
        Boolean enabled
) {
}
