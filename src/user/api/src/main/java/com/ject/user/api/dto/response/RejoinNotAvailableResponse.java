package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "MEMBER_002 실패 응답의 data. 같은 소셜 계정으로 다시 가입할 수 있는 시각")
public record RejoinNotAvailableResponse(
        @Schema(description = "다시 가입할 수 있는 시각 (탈퇴 시각 + 7일)", example = "2026-11-22T14:30:00+09:00")
        OffsetDateTime rejoinAvailableAt
) {
}
