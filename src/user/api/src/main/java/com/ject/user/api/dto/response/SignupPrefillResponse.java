package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "가입 화면 자동 채우기 값. 자동 채우기 화면은 2차라 1차 FE는 무시해도 됨")
public record SignupPrefillResponse(
        @Schema(description = "생년월일. 네이버만 채움, 카카오 · Apple은 항상 null", example = "2001-03-15", nullable = true)
        LocalDate birthDate,

        @Schema(description = "성별. 네이버만 채움, 카카오 · Apple은 항상 null", example = "FEMALE", nullable = true)
        Gender gender
) {
}
