package com.ject.user.api.dto.request;

import com.ject.user.api.dto.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "회원 가입 완료. 약관 동의 + 프로필을 한 번에 보냄")
public record SignupRequest(
        @Schema(description = "AU-1에서 받은 가입 토큰 (7일)", example = "eyJhbGciOiJIUzI1NiJ9.signup")
        @NotBlank
        String signupToken,

        @Schema(description = "약관 동의. TM-1에서 signupConsent가 REQUIRED · OPTIONAL인 약관을 모두 보냄")
        @NotEmpty
        List<@NotNull @Valid TermsAgreementRequest> agreements,

        @Schema(description = "닉네임. 2~10자, 완성형 한글 · 영문 · 숫자 (MB-1 규칙)", example = "쿠로미덕후")
        @NotBlank
        String nickname,

        @Schema(description = "생년월일 YYYY-MM-DD. 1900-01-01 ~ 오늘(KST), 한국 시간 기준 만 14세 이상. 가입 후 수정 불가", example = "2001-03-15")
        @NotNull
        LocalDate birthDate,

        @Schema(description = "성별. 선택, 비워 두면 null. 가입 후 수정 불가", example = "FEMALE", nullable = true)
        Gender gender,

        @Schema(description = "기기 ID (DV-1과 같은 값)", example = "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        @NotBlank
        String deviceId
) {
}
