package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "차단한 회원")
public record BlockedMemberResponse(
        @Schema(description = "회원 ID", example = "202")
        Long userId,

        @Schema(description = "닉네임", example = "하치와레러버")
        String nickname,

        @Schema(description = "차단 시각", example = "2026-11-27T14:30:00+09:00")
        OffsetDateTime blockedAt
) {
}
