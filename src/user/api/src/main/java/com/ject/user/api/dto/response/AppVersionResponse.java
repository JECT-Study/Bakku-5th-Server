package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 최소 지원 버전")
public record AppVersionResponse(
        @Schema(description = "최소 지원 버전. 이보다 낮으면 강제 업데이트", example = "1.0.0")
        String minimumVersion,

        @Schema(description = "스토어 앱 페이지 URL", example = "https://apps.apple.com/app/id0000000000")
        String storeUrl
) {
}
