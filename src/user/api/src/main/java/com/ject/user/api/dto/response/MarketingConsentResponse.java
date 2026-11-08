package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "마케팅 수신 동의")
public record MarketingConsentResponse(
        @Schema(description = "바뀐 값", example = "false")
        boolean agreed,

        @Schema(description = "변경 일시", example = "2026-11-27T14:30:00+09:00")
        OffsetDateTime changedAt
) {
}
