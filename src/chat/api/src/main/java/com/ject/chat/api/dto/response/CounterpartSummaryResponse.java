package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 목록의 상대 요약")
public record CounterpartSummaryResponse(
        @Schema(description = "상대 사용자 ID", example = "202")
        Long userId,

        @Schema(description = "상대 닉네임", example = "하치와레러버")
        String nickname,

        @Schema(description = "상대가 나간 방이면 true → \"상대가 나갔어요\" 표시", example = "false")
        boolean left
) {
}
