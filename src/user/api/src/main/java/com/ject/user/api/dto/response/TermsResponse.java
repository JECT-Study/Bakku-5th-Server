package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.TermsConsent;
import com.ject.user.api.dto.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "약관")
public record TermsResponse(
        @Schema(description = "약관 종류", example = "SERVICE")
        TermsType termsType,

        @Schema(description = "화면에 보일 이름", example = "서비스 이용약관")
        String title,

        @Schema(description = "현재 버전. 가입 때 이 값을 그대로 보냄", example = "1.0")
        String version,

        @Schema(description = "가입 때 필수 · 선택 · 받지 않음", example = "REQUIRED")
        TermsConsent signupConsent,

        @Schema(description = "본문 웹뷰 URL. 만 14세 확인처럼 본문이 없으면 null", example = "https://{도메인}/terms/service/1.0", nullable = true)
        String url,

        @Schema(description = "시행일", example = "2026-11-27")
        LocalDate effectiveAt
) {
}
