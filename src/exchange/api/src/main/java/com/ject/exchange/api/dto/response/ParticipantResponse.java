package com.ject.exchange.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교환 참여자")
public record ParticipantResponse(
        @Schema(description = "사용자 ID", example = "101")
        Long userId,

        @Schema(description = "닉네임", example = "쿠로미덕후")
        String nickname,

        @Schema(description = "교환 완료 횟수 (교환 n회)", example = "12")
        Integer completedCount
) {
}
