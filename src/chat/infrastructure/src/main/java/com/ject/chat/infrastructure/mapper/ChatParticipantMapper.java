package com.ject.chat.infrastructure.mapper;

import com.ject.chat.domain.domain.chatroom.ChatParticipant;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.infrastructure.entity.ChatParticipantEntity;
import com.ject.chat.infrastructure.entity.ChatRoomEntity;

import java.util.List;

public class ChatParticipantMapper {

    public static List<ChatParticipantEntity> toEntities(ChatRoomEntity chatRoomEntity, ChatRoom chatRoom) {
        return chatRoom.getParticipants().stream()
                .map(participant -> toEntity(chatRoomEntity, participant))
                .toList();
    }

    public static ChatParticipantEntity toEntity(ChatRoomEntity chatRoomEntity, ChatParticipant participant) {
        return ChatParticipantEntity.builder()
                .id(participant.getId())
                .chatRoom(chatRoomEntity)
                .memberId(participant.getMemberId())
                .role(participant.getRole())
                .leftAt(participant.getLeftAt())
                .build();
    }

    public static ChatParticipant toDomain(ChatParticipantEntity entity) {
        return ChatParticipant.of(
                entity.getId(),
                entity.getChatRoom().getId(),
                entity.getMemberId(),
                entity.getRole(),
                entity.getLeftAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
