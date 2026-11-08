package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인. 카카오 · 네이버는 socialAccessToken, Apple은 identityToken + authorizationCode")
public record SocialLoginRequest(
        @Schema(description = "카카오 · 네이버 SDK 액세스 토큰. naver · kakao일 때 필수", example = "AAAANaverOrKakaoAccessToken")
        String socialAccessToken,

        @Schema(description = "Apple identity token(JWT). apple일 때 필수", example = "eyJraWQiOiJBSURPUEsxIn0.apple")
        String identityToken,

        @Schema(description = "Apple authorization code. apple일 때 필수 (탈퇴 때 Apple 토큰 취소에 사용)", example = "c1a2b3c4d5.0.apple")
        String authorizationCode,

        @Schema(description = "기기 ID (DV-1과 같은 값). 리프레시 토큰을 기기마다 1개로 관리", example = "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        @NotBlank
        String deviceId
) {
}
