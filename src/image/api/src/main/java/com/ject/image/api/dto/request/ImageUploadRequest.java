package com.ject.image.api.dto.request;

import com.ject.image.api.dto.enums.ImagePurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "이미지 업로드")
public record ImageUploadRequest(
        @Schema(description = "용도. key 앞부분과 볼 수 있는 사람이 정해짐", example = "POST")
        @NotNull
        ImagePurpose purpose,

        @Schema(description = "파일 1~5개, 고른 순서대로. JPEG · PNG · WEBP, 장당 최대 10MB")
        @NotNull @Size(min = 1, max = 5)
        List<MultipartFile> files
) {
}
