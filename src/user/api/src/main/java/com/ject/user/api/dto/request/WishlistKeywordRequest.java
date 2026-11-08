package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "위시리스트 키워드 추가 (2차)")
public record WishlistKeywordRequest(
        @Schema(description = "키워드 2~20자", example = "치이카와")
        @NotBlank @Size(min = 2, max = 20)
        String keyword
) {
}
