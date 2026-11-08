package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.LogoutRequest;
import com.ject.user.api.dto.request.SignupRequest;
import com.ject.user.api.dto.request.SocialLoginRequest;
import com.ject.user.api.dto.request.TokenRefreshRequest;
import com.ject.user.api.dto.response.LoginResponse;
import com.ject.user.api.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Auth", description = "소셜 로그인 · 가입 완료 · 토큰 재발급 · 로그아웃")
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @Operation(summary = "AU-1 소셜 로그인",
            description = "소셜 인증 결과로 회원인지 확인합니다. 기존 회원이면 토큰을 발급하고(LOGIN), 처음이면 가입 토큰을 돌려줍니다(SIGNUP_REQUIRED). "
                    + "stub은 토큰 값이 new로 시작하면 SIGNUP_REQUIRED, 아니면 LOGIN을 응답합니다. 인증 없이 호출합니다. "
                    + "에러: MEMBER_001 소셜 토큰 검증 실패 · MEMBER_002 탈퇴 후 7일 안 (rejoinAvailableAt) · MEMBER_003 영구 정지")
    @SecurityRequirements
    @PostMapping("/{provider}/login")
    ApiResponse<LoginResponse> login(
            @Parameter(description = "naver / kakao / apple (apple은 iOS만)", example = "kakao")
            @PathVariable("provider") @Pattern(regexp = "naver|kakao|apple", message = "naver, kakao, apple 중 하나여야 합니다.") String provider,
            @Valid @RequestBody SocialLoginRequest request);

    @Operation(summary = "AU-2 회원 가입 완료",
            description = "약관 동의와 프로필(닉네임 · 생년월일 · 성별)을 한 번에 받아 회원을 만들고 토큰을 발급합니다. 인증 대신 Body의 가입 토큰을 씁니다. "
                    + "에러: MEMBER_010 가입 토큰 만료 · MEMBER_011 필수 약관 미동의 · MEMBER_012 약관 버전 오래됨 · MEMBER_013 만 14세 미만 · "
                    + "MEMBER_014 이미 가입됨 · MEMBER_101 닉네임 형식 · MEMBER_102 닉네임 중복")
    @SecurityRequirements
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request);

    @Operation(summary = "AU-3 토큰 재발급",
            description = "액세스 토큰이 만료되면(AUTH_002) 호출합니다. 리프레시 토큰도 새로 발급하고 이전 것은 무효로 합니다. FE는 재발급을 한 번에 하나만 보냅니다. 인증 없이 호출합니다. "
                    + "에러: AUTH_004 리프레시 토큰 무효 → 로그인 화면 · AUTH_005 직전 토큰이 10초 안에 다시 옴 → 새 토큰으로 재시도 · MEMBER_003 영구 정지")
    @SecurityRequirements
    @PostMapping("/token/refresh")
    ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request);

    @Operation(summary = "AU-4 로그아웃",
            description = "리프레시 토큰과 이 기기의 푸시 토큰을 지웁니다. 서버 요청이 실패해도 FE는 기기 로그인 정보를 지우고 로그인 화면으로 갑니다.")
    @PostMapping("/logout")
    ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request);
}
