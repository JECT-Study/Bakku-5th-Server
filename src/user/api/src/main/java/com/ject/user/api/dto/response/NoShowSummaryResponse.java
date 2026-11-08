package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노쇼 기록 요약 (2차). 건별 목록은 MB-8")
public record NoShowSummaryResponse(
        @Schema(description = "인정된 노쇼 수", example = "1")
        int confirmedCount,

        @Schema(description = "확인 중인 노쇼 수", example = "0")
        int pendingCount
) {
}
