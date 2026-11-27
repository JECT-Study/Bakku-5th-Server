package com.ject.chat.domain.repository;

import com.ject.chat.domain.domain.chatroom.ChatRoom;

import java.util.Optional;

public interface ChatRoomRepository {

    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(Long id);

    /**
     * 동일 게시글·동일 요청자가 만든 방을 찾는다. 나간 방도 포함한다
     */
    Optional<ChatRoom> findByPostIdAndRequesterId(Long postId, Long requesterId);

    /**
     * 교환 ID로 연결된 채팅방 ID만 조회한다
     */
    Optional<Long> findChatRoomIdByExchangeId(Long exchangeId);

    /**
     * 게시글에서 요청자가 나가지 않은 방의 수를 센다
     */
    int countActiveRequestsByPostId(Long postId);
}
