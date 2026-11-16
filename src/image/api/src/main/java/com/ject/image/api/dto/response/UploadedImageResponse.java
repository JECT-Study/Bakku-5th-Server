package com.ject.image.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드한 이미지")
public record UploadedImageResponse(
        @Schema(description = "이미지 key. 저장 API(PO-1 등)에 그대로 전달", example = "post/tmp/3/8f14e45f.jpg")
        String key
) {
}
