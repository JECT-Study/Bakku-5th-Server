package com.ject.chat.domain.repository;

import com.ject.chat.domain.domain.message.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    /**
     * 과거 조회. beforeMessageId보다 작은 메시지를 id 내림차순으로 limit개 조회한다. beforeMessageId가 없으면 처음부터
     */
    List<ChatMessage> findRecent(Long chatRoomId, Long beforeMessageId, int limit);

    /**
     * 연결 복구 조회. afterMessageId보다 큰 메시지를 id 오름차순 전체 조회한다
     */
    List<ChatMessage> findAfter(Long chatRoomId, Long afterMessageId);

    /**
     * cursor·after 기준 메시지가 이 채팅방에 속하는지 확인한다
     */
    boolean existsByIdAndChatRoomId(Long messageId, Long chatRoomId);
}
