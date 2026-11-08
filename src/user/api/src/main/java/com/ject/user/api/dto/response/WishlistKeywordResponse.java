package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위시리스트 키워드 (2차)")
public record WishlistKeywordResponse(
        @Schema(description = "키워드 ID", example = "1")
        Long keywordId,

        @Schema(description = "키워드", example = "치이카와")
        String keyword
) {
}
