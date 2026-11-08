package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "차단 결과")
public record BlockResponse(
        @Schema(description = "차단한 회원 ID", example = "202")
        Long userId,

        @Schema(description = "차단 시각. 이미 차단한 회원이면 기존 차단 시각", example = "2026-11-27T14:30:00+09:00")
        OffsetDateTime blockedAt
) {
}
