package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "닉네임 수정 결과 (2차)")
public record NicknameUpdateResponse(
        @Schema(description = "바뀐 닉네임", example = "하치와레러버")
        String nickname,

        @Schema(description = "다음 변경 가능 시각 (지금 + 30일)", example = "2026-12-27T14:30:00+09:00")
        OffsetDateTime nicknameChangeableAt
) {
}
