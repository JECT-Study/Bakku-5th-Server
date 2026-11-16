package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "dev 테스트 토큰")
public record DevTokenResponse(
        @Schema(description = "API 호출용 토큰. Authorization: Bearer {accessToken}")
        String accessToken,
        @Schema(description = "액세스 토큰 만료 시각 (발급 후 1시간)", example = "2026-11-12T15:30:00+09:00")
        OffsetDateTime accessTokenExpiresAt,
        @Schema(description = "테스트 회원")
        LoginMemberResponse member
) {
}
