package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.ConfirmationAnswer;
import com.ject.exchange.api.dto.enums.ConfirmationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "교환 확인 정보")
public record ConfirmationResponse(
        @Schema(description = "교환 확인 카드 발송 시각 (약속 시각 + 30분). 발송 전이면 null", example = "2026-10-27T15:30:00+09:00")
        OffsetDateTime askedAt,

        @Schema(description = "교환 확인 결과. 결과 전이면 null")
        ConfirmationResult result,

        @Schema(description = "3일 무응답 자동 완료 여부", example = "false")
        boolean autoCompleted,

        @Schema(description = "내 응답. 응답 전이면 null")
        ConfirmationAnswer myAnswer,

        @Schema(description = "결과 확정 시각. 결과 전이면 null")
        OffsetDateTime resolvedAt
) {
}
