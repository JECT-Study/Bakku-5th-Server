package com.ject.chat.application.dto;

import java.util.List;

/**
 * CH-3 메시지 이력 조회 결과 한 페이지
 *
 * @param nextCursorMessageId 다음 페이지 커서(마지막 메시지 id). after 모드이거나 마지막 페이지면 null
 */
public record ChatMessagePage(List<ChatMessageResult> content, Long nextCursorMessageId, boolean hasNext) {
}
