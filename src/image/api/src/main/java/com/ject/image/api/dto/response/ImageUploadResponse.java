package com.ject.image.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "업로드 결과")
public record ImageUploadResponse(
        @Schema(description = "보낸 files 순서대로")
        List<UploadedImageResponse> uploads
) {
}
