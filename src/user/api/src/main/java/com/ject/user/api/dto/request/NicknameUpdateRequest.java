package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "닉네임 수정 (2차)")
public record NicknameUpdateRequest(
        @Schema(description = "새 닉네임 (MB-1 규칙)", example = "하치와레러버")
        @NotBlank
        String nickname
) {
}
