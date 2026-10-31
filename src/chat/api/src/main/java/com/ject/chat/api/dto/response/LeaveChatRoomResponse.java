package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 나가기 결과")
public record LeaveChatRoomResponse(
        @Schema(description = "나간 채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "확정 약속이 일방 취소되었는지", example = "true")
        boolean appointmentCanceled,

        @Schema(description = "임박 취소로 매너 지표에 반영되었는지. 토스트 \"매너 지표에 반영돼요\" 추가 여부", example = "false")
        boolean penaltyApplied
) {
}
