package com.ject.chat.application.dto;

import java.time.Instant;

/**
 * CH-3 메시지 이력 조회 결과 한 건
 *
 * @param senderUserId    보낸 사람. SYSTEM · CARD는 null
 * @param clientMessageId 보낸 쪽이 만든 ID. SYSTEM · CARD는 null
 */
public record ChatMessageResult(
        Long messageId,
        Long chatRoomId,
        Long senderUserId,
        ChatMessageType type,
        ChatMessageContentResult content,
        String clientMessageId,
        Instant sentAt
) {
}
