package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "dev 테스트 토큰 발급")
public record DevTokenRequest(
        @Schema(description = "테스트 회원 닉네임. 있으면 그 회원으로, 없으면 새로 만들어 발급. 비우면 '테스트+숫자'로 새 회원",
                example = "테스트요청자", nullable = true)
        String nickname
) {
}
