package com.ject.chat.application.dto.query;

import com.ject.chat.application.dto.ChatRoomDetailResult;
import com.ject.common.core.cqrs.query.Query;
import lombok.Builder;

/**
 * CH-2 채팅방 상세 조회. 조회 회원({@code viewerId})이 현재 참여자가 아니면 CHAT_002
 */
public record GetChatRoomDetailQuery(
        Long chatRoomId,
        Long viewerId
) implements Query<ChatRoomDetailResult> {
    @Builder
    public GetChatRoomDetailQuery {
    }
}
