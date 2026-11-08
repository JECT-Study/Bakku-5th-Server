package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신고 결과")
public record ReportResponse(
        @Schema(description = "신고 ID", example = "31")
        Long reportId
) {
}
