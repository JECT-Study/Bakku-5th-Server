package com.ject.exchange.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "약속 수락")
public record AcceptAppointmentRequest(
        @Schema(description = "수락자 본인의 약속 전 알림(분). null 끄기, 10분 단위 10~1440. 지금 바로 약속이면 무시", example = "30")
        @Min(10) @Max(1440)
        Integer reminderOffsetMinutes
) {
}
