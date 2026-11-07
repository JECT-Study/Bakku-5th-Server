package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시자")
public record AuthorResponse(
        @Schema(description = "사용자 ID. 탈퇴한 사용자면 null (프로필 이동 안 함)", example = "3")
        Long userId,

        @Schema(description = "닉네임. 탈퇴한 사용자면 '탈퇴한 사용자'", example = "하치와레러버")
        String nickname,

        @Schema(description = "교환 완료 횟수. 매너 지표는 2차", example = "12")
        int completedCount
) {
}
