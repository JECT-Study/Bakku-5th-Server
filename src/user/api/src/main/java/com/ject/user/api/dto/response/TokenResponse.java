package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "발급한 토큰")
public record TokenResponse(
        @Schema(description = "API 호출용 토큰. Authorization: Bearer {accessToken}", example = "eyJhbGciOiJIUzI1NiJ9.access")
        String accessToken,

        @Schema(description = "액세스 토큰 만료 시각 (발급 후 1시간)", example = "2026-10-29T15:30:00+09:00")
        OffsetDateTime accessTokenExpiresAt,

        @Schema(description = "재발급용 토큰. 시간으로는 만료되지 않음 (로그아웃 · 탈퇴 · 정지 · 재사용 감지 때만 무효)", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
        String refreshToken,

        @Schema(description = "로그인한 회원")
        LoginMemberResponse member
) {
}
