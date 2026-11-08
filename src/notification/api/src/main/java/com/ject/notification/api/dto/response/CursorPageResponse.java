package com.ject.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "커서 기반 목록")
public record CursorPageResponse<T>(
        @Schema(description = "목록")
        List<T> content,

        @Schema(description = "다음 페이지 커서. 마지막이면 null", example = "eyJpZCI6OTAwMH0", nullable = true)
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
