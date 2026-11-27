package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.MannerTier;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다른 회원 프로필. 닉네임 · 교환 완료 횟수 · 매너 지표 구간 이름만 공개")
public record MemberProfileResponse(
        @Schema(description = "회원 ID", example = "202")
        Long userId,

        @Schema(description = "닉네임", example = "하치와레러버")
        String nickname,

        @Schema(description = "교환 완료 횟수. 0이면 '첫 교환을 기다리고 있어요'", example = "5")
        int completedCount,

        @Schema(description = "내 프로필이면 true → ⋮ 메뉴 숨김", example = "false")
        boolean isMe,

        @Schema(description = "매너 지표 구간 이름. PRO_COLLECTOR 프로컬렉터 · GOODS_HUNTER 굿즈헌터 · TRAINEE 교환연습생. "
                + "숫자는 다른 사람에게 보이지 않음 (F-06-1). 교환 0회면 null", example = "GOODS_HUNTER", nullable = true)
        MannerTier mannerTier
) {
}
