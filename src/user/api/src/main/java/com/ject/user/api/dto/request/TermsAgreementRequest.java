package com.ject.user.api.dto.request;

import com.ject.user.api.dto.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "약관 동의")
public record TermsAgreementRequest(
        @Schema(description = "약관 종류", example = "SERVICE")
        @NotNull
        TermsType termsType,

        @Schema(description = "동의한 약관 버전 (TM-1의 version)", example = "1.0")
        @NotBlank
        String version,

        @Schema(description = "동의 여부. 필수 약관은 true여야 함", example = "true")
        @NotNull
        Boolean agreed
) {
}
