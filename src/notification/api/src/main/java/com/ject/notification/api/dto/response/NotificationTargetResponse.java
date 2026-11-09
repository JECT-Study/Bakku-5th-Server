package com.ject.notification.api.dto.response;

import com.ject.common.event.notification.NotificationTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림을 누르면 이동할 대상")
public record NotificationTargetResponse(
        @Schema(description = "대상 종류", example = "CHAT_ROOM")
        NotificationTargetType type,

        @Schema(description = "대상 ID. SEARCH면 null", example = "5", nullable = true)
        Long id,

        @Schema(description = "SEARCH일 때 검색어 (2차 키워드 묶음 알림)", nullable = true)
        String keyword
) {
}
