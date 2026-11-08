package com.ject.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모두 읽음 결과")
public record ReadAllResponse(
        @Schema(description = "이번에 읽음 처리한 수", example = "3")
        long readCount
) {
}
