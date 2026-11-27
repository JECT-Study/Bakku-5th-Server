package com.ject.chat.application.service;

import com.ject.chat.application.command.ChatRoomCommand;
import com.ject.chat.application.dto.ChatRoomLink;
import com.ject.chat.application.dto.ChatRoomLinkStatus;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.repository.ChatRoomLockRepository;
import com.ject.chat.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomCommandService implements ChatRoomCommand {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomLockRepository chatRoomLockRepository;

    @Override
    @Transactional
    public ChatRoomLink lockAndFind(Long postId, Long requesterId) {
        chatRoomLockRepository.lock(postId, requesterId);
        Optional<ChatRoom> existing = chatRoomRepository.findByPostIdAndRequesterId(postId, requesterId);
        if (existing.isEmpty()) {
            return new ChatRoomLink(null, null, ChatRoomLinkStatus.ABSENT);
        }
        ChatRoom chatRoom = existing.get();
        if (chatRoom.isActiveForRequester()) {
            return new ChatRoomLink(chatRoom.getExchangeId(), chatRoom.getId(), ChatRoomLinkStatus.ACTIVE);
        }
        return new ChatRoomLink(chatRoom.getExchangeId(), null, ChatRoomLinkStatus.LEFT);
    }

    @Override
    @Transactional
    public Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        ChatRoom chatRoom = ChatRoom.create(exchangeId, postId, requesterId, ownerId);
        return chatRoomRepository.save(chatRoom).getId();
    }
}
