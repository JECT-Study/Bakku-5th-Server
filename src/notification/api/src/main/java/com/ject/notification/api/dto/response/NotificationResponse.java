package com.ject.notification.api.dto.response;

import com.ject.notification.api.dto.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "알림")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "9001")
        Long notificationId,

        @Schema(description = "알림 종류", example = "APPOINTMENT_PROPOSED")
        NotificationType type,

        @Schema(description = "제목 (서버 생성)", example = "교환 약속 제안이 왔어요")
        String title,

        @Schema(description = "내용 (서버 생성)", example = "하치와레러버님이 12/2(화) 15:00 홍대입구역에서 만나자고 했어요")
        String body,

        @Schema(description = "누르면 이동할 대상")
        NotificationTargetResponse target,

        @Schema(description = "읽음 여부. 안 읽은 알림은 강조", example = "false")
        boolean read,

        @Schema(description = "받은 시각", example = "2026-12-01T09:00:00+09:00")
        OffsetDateTime createdAt
) {
}
