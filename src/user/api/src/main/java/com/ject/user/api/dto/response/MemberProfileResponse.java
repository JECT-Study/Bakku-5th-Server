package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다른 회원 프로필 (2차). 닉네임과 교환 완료 횟수만 공개")
public record MemberProfileResponse(
        @Schema(description = "회원 ID", example = "202")
        Long userId,

        @Schema(description = "닉네임", example = "하치와레러버")
        String nickname,

        @Schema(description = "교환 완료 횟수. 0이면 '첫 교환을 기다리고 있어요'", example = "5")
        int completedCount,

        @Schema(description = "내 프로필이면 true → ⋮ 메뉴 숨김", example = "false")
        boolean isMe,

        @Schema(description = "매너 지표. 계산 · 표시는 2차에 정함", nullable = true)
        Double mannerScore
) {
}
