package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽음 처리 결과")
public record ReadMessagesResponse(
        @Schema(description = "채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "저장된 읽음 위치 (요청값보다 클 수 있음)", example = "121")
        Long lastReadMessageId,

        @Schema(description = "갱신 후 안 읽은 수", example = "0")
        Integer unreadCount
) {
}
