package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "받은 후기 단계별 개수 (2차)")
public record ReceivedReviewsResponse(
        @Schema(description = "최고예요", example = "3")
        int best,

        @Schema(description = "좋아요", example = "5")
        int good,

        @Schema(description = "별로예요", example = "0")
        int bad
) {
}
