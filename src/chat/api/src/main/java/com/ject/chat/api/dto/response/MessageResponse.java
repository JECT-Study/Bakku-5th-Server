package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "메시지")
public record MessageResponse(
        @Schema(description = "메시지 ID (방 안에서 시간순 증가)", example = "121")
        Long messageId,

        @Schema(description = "채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "보낸 사람. 시스템 · 카드 메시지는 null", example = "101")
        Long senderUserId,

        @Schema(description = "메시지 타입", example = "TEXT")
        MessageType type,

        @Schema(description = "타입별 내용")
        MessageContent content,

        @Schema(description = "보낸 쪽이 만든 ID. 시스템 · 카드 메시지는 null", example = "2f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        String clientMessageId,

        @Schema(description = "보낸 시각", example = "2026-10-12T14:31:00+09:00")
        OffsetDateTime sentAt
) {
}
