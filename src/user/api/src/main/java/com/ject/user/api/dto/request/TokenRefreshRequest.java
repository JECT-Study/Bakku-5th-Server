package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급")
public record TokenRefreshRequest(
        @Schema(description = "저장해 둔 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
        @NotBlank
        String refreshToken
) {
}
