package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "위시리스트 키워드 목록 (2차)")
public record WishlistKeywordListResponse(
        @Schema(description = "등록 순서")
        List<WishlistKeywordResponse> keywords,

        @Schema(description = "최대 개수", example = "10")
        int maxCount,

        @Schema(description = "키워드 알림 켜짐 여부 (알림 설정의 WISHLIST_KEYWORD)", example = "true")
        boolean notificationEnabled
) {
}
