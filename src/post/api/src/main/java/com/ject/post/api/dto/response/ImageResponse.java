package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교환글 사진")
public record ImageResponse(
        @Schema(description = "이미지 key. 수정 때 그대로 보냄", example = "post/10/8f14e45f.jpg")
        String key,

        @Schema(description = "이미지 조회 URL", example = "https://{cdn}/post/10/8f14e45f.jpg")
        String url
) {
}
