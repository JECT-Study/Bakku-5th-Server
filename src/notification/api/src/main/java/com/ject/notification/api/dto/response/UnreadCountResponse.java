package com.ject.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "안 읽은 알림 수")
public record UnreadCountResponse(
        @Schema(description = "안 읽은 알림 수 (30일 안). 99 초과 표시는 FE", example = "3")
        long count
) {
}
