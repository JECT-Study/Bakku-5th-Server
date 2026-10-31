package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.CancellationStatus;
import com.ject.exchange.api.dto.enums.CancellationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "약속 취소")
public record CancellationResponse(
        @Schema(description = "취소 ID", example = "7")
        Long id,

        @Schema(description = "약속 ID", example = "3")
        Long appointmentId,

        @Schema(description = "취소를 시작한 사람", example = "101")
        Long requestedByUserId,

        @Schema(description = "취소가 시작된 경로", example = "REQUEST")
        CancellationType type,

        @Schema(description = "취소 상태", example = "REQUESTED")
        CancellationStatus status,

        @Schema(description = "임박 여부. 시간 지정 약속은 1시간 전부터, 지금 바로 약속은 확정 순간부터 true", example = "false")
        boolean imminent,

        @Schema(description = "상대 무응답 시 일방 취소가 가능해지는 시각. type이 REQUEST가 아니면 null", example = "2026-10-27T13:30:00+09:00")
        OffsetDateTime forceAvailableAt,

        @Schema(description = "취소 시작 시각", example = "2026-10-27T13:00:00+09:00")
        OffsetDateTime requestedAt,

        @Schema(description = "처리 완료 시각. 처리 전이면 null")
        OffsetDateTime resolvedAt
) {
}
