package com.ject.chat.infrastructure.repository;

import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.repository.ChatRoomRepository;
import com.ject.chat.infrastructure.entity.ChatParticipantEntity;
import com.ject.chat.infrastructure.entity.ChatRoomEntity;
import com.ject.chat.infrastructure.jpa.ChatParticipantEntityRepository;
import com.ject.chat.infrastructure.jpa.ChatRoomEntityRepository;
import com.ject.chat.infrastructure.mapper.ChatParticipantMapper;
import com.ject.chat.infrastructure.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaChatRoomRepository implements ChatRoomRepository {

    private final ChatRoomEntityRepository chatRoomEntityRepository;
    private final ChatParticipantEntityRepository chatParticipantEntityRepository;

    @Override
    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomEntity savedRoom = chatRoomEntityRepository.save(ChatRoomMapper.toEntity(chatRoom));
        chatParticipantEntityRepository.saveAll(ChatParticipantMapper.toEntities(savedRoom, chatRoom));
        return findById(savedRoom.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public Optional<ChatRoom> findById(Long id) {
        return chatRoomEntityRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<ChatRoom> findByPostIdAndRequesterId(Long postId, Long requesterId) {
        return chatRoomEntityRepository.findByPostIdAndRequesterId(postId, requesterId).map(this::toDomain);
    }

    @Override
    public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
        return chatRoomEntityRepository.findIdByExchangeId(exchangeId);
    }

    @Override
    public int countActiveRequestsByPostId(Long postId) {
        return chatParticipantEntityRepository.countByChatRoomPostIdAndRoleAndLeftAtIsNull(
                postId, ChatParticipantRole.REQUESTER);
    }

    private ChatRoom toDomain(ChatRoomEntity entity) {
        List<ChatParticipantEntity> participants = chatParticipantEntityRepository.findByChatRoomId(entity.getId());
        return ChatRoomMapper.toDomain(entity, participants);
    }
}
