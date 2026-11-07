package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주변 글 수")
public record PostCountResponse(
        @Schema(description = "반경 1km 안 글 수", example = "12")
        int count
) {
}
