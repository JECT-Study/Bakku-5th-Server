package com.ject.chat.application.service;

import com.ject.chat.application.dto.PostChatRoomInfo;
import com.ject.chat.application.query.PostChatRoomQuery;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostChatRoomQueryService implements PostChatRoomQuery {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public PostChatRoomInfo getPostChatRoomInfo(Long userId, Long postId) {
        Optional<ChatRoom> room = chatRoomRepository.findByPostIdAndRequesterId(postId, userId);
        int requestCount = chatRoomRepository.countActiveRequestsByPostId(postId);
        if (room.isEmpty()) {
            return new PostChatRoomInfo(null, false, requestCount);
        }
        ChatRoom chatRoom = room.get();
        if (chatRoom.isActiveForRequester()) {
            return new PostChatRoomInfo(chatRoom.getId(), false, requestCount);
        }
        return new PostChatRoomInfo(null, true, requestCount);
    }
}
