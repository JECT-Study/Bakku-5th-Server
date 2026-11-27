package com.ject.chat.infrastructure.mapper;

import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.infrastructure.entity.ChatParticipantEntity;
import com.ject.chat.infrastructure.entity.ChatRoomEntity;

import java.util.List;

public class ChatRoomMapper {

    public static ChatRoomEntity toEntity(ChatRoom chatRoom) {
        return ChatRoomEntity.builder()
                .id(chatRoom.getId())
                .exchangeId(chatRoom.getExchangeId())
                .postId(chatRoom.getPostId())
                .requesterId(chatRoom.getRequesterId())
                .build();
    }

    public static ChatRoom toDomain(ChatRoomEntity entity, List<ChatParticipantEntity> participants) {
        return ChatRoom.of(
                entity.getId(),
                entity.getExchangeId(),
                entity.getPostId(),
                entity.getRequesterId(),
                participants.stream().map(ChatParticipantMapper::toDomain).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
