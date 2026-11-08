package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그아웃")
public record LogoutRequest(
        @Schema(description = "지울 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
        @NotBlank
        String refreshToken,

        @Schema(description = "DV-1에 등록한 기기 ID. 이 기기의 푸시 토큰도 지움", example = "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        @NotBlank
        String deviceId
) {
}
