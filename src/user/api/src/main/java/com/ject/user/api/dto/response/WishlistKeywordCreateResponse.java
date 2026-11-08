package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위시리스트 키워드 추가 결과 (2차)")
public record WishlistKeywordCreateResponse(
        @Schema(description = "키워드 ID", example = "2")
        Long keywordId,

        @Schema(description = "저장된 키워드", example = "우사기")
        String keyword,

        @Schema(description = "금지어 포함 여부. 저장하고 경고만", example = "false")
        boolean bannedWordDetected
) {
}
