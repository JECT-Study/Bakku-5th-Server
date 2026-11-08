package com.ject.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 읽음 결과")
public record NotificationReadResponse(
        @Schema(description = "알림 ID", example = "9001")
        Long notificationId,

        @Schema(description = "항상 true", example = "true")
        boolean read
) {
}
