package com.ject.chat.api.dto.response;

import com.ject.chat.application.dto.CounterpartDetail;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 상단 바의 상대 정보")
public record CounterpartResponse(
        @Schema(description = "상대 사용자 ID", example = "202")
        Long userId,

        @Schema(description = "상대 닉네임", example = "하치와레러버")
        String nickname,

        @Schema(description = "상대 교환 완료 횟수. 상단 바 \"교환 n회\"", example = "5")
        Integer completedCount,

        @Schema(description = "상대가 나갔는지", example = "false")
        boolean left
) {

    public static CounterpartResponse from(CounterpartDetail detail) {
        return new CounterpartResponse(detail.userId(), detail.nickname(), detail.completedCount(), detail.left());
    }
}
