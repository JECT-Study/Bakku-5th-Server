package com.ject.exchange.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "내 약속 전 알림 변경")
public record UpdateReminderRequest(
        @Schema(description = "약속 전 알림(분). null 끄기, 10분 단위 10~1440 (최대 하루 전)", example = "60")
        @Min(10) @Max(1440)
        Integer reminderOffsetMinutes
) {
}
