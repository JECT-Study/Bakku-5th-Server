package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.AppointmentChangeStatus;
import com.ject.exchange.api.dto.enums.AppointmentTimeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "약속 변경 제안")
public record AppointmentChangeResponse(
        @Schema(description = "변경 제안 ID", example = "9")
        Long id,

        @Schema(description = "약속 ID", example = "3")
        Long appointmentId,

        @Schema(description = "변경을 제안한 사람. 상대만 수락 · 거절 가능", example = "202")
        Long proposerUserId,

        @Schema(description = "변경 제안 상태", example = "PENDING")
        AppointmentChangeStatus status,

        @Schema(description = "바꿀 시간 유형", example = "SCHEDULED")
        AppointmentTimeType timeType,

        @Schema(description = "바꿀 약속 시각", example = "2026-10-27T16:00:00+09:00")
        OffsetDateTime scheduledAt,

        @Schema(description = "바꿀 장소")
        PlaceResponse place,

        @Schema(description = "바꿀 교환할 물건", example = "[31]")
        List<Long> offeredItemIds,

        @Schema(description = "제안 시각", example = "2026-10-27T10:00:00+09:00")
        OffsetDateTime requestedAt,

        @Schema(description = "처리 시각. 처리 전이면 null")
        OffsetDateTime resolvedAt
) {
}
