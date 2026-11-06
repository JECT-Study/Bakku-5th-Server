package com.ject.exchange.api.dto.request;

import com.ject.exchange.api.dto.enums.AppointmentTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "확정 약속 변경 제안")
public record ProposeAppointmentChangeRequest(
        @Schema(description = "NOW 지금 바로 / SCHEDULED 날짜 · 시간 지정", example = "SCHEDULED")
        @NotNull
        AppointmentTimeType timeType,

        @Schema(description = "SCHEDULED일 때 필수. 미래 시각, 분이 10의 배수, 오늘~7일 후. NOW면 보내지 않음", example = "2026-10-27T16:00:00+09:00")
        OffsetDateTime scheduledAt,

        @Schema(description = "약속 장소")
        @NotNull @Valid
        PlaceRequest place,

        @Schema(description = "교환할 물건 (이 교환의 첨부 아이템 ID, 중복 불가). 고를 수 있는 아이템이 1개면 생략 가능", example = "[31]")
        @Size(min = 1)
        List<@NotNull Long> offeredItemIds
) {
}
