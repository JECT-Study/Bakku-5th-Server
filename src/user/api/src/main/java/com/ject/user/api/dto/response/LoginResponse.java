package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "소셜 로그인 결과. LOGIN이면 토큰 · 회원, SIGNUP_REQUIRED면 가입 토큰 · 자동 채우기 값만 채움")
public record LoginResponse(
        @Schema(description = "LOGIN 기존 회원 / SIGNUP_REQUIRED 가입 필요", example = "LOGIN")
        LoginResult result,

        @Schema(description = "API 호출용 토큰. Authorization: Bearer {accessToken}. LOGIN일 때만", example = "eyJhbGciOiJIUzI1NiJ9.access", nullable = true)
        String accessToken,

        @Schema(description = "액세스 토큰 만료 시각 (발급 후 1시간). LOGIN일 때만", example = "2026-10-29T15:30:00+09:00", nullable = true)
        OffsetDateTime accessTokenExpiresAt,

        @Schema(description = "재발급용 토큰. 시간으로는 만료되지 않음 (로그아웃 · 탈퇴 · 정지 · 재사용 감지 때만 무효). LOGIN일 때만", example = "eyJhbGciOiJIUzI1NiJ9.refresh", nullable = true)
        String refreshToken,

        @Schema(description = "로그인한 회원. LOGIN일 때만", nullable = true)
        LoginMemberResponse member,

        @Schema(description = "가입 완료(AU-2)에 보낼 토큰. 유효 7일. SIGNUP_REQUIRED일 때만", example = "eyJhbGciOiJIUzI1NiJ9.signup", nullable = true)
        String signupToken,

        @Schema(description = "가입 토큰 만료 시각. SIGNUP_REQUIRED일 때만", example = "2026-11-05T14:30:00+09:00", nullable = true)
        OffsetDateTime signupTokenExpiresAt,

        @Schema(description = "가입 화면 자동 채우기 값. SIGNUP_REQUIRED일 때만", nullable = true)
        SignupPrefillResponse prefill
) {
}
