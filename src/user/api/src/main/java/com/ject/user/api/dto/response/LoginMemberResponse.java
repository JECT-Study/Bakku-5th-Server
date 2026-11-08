package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인한 회원")
public record LoginMemberResponse(
        @Schema(description = "회원 ID", example = "101")
        Long userId,

        @Schema(description = "닉네임", example = "쿠로미덕후")
        String nickname
) {
}
