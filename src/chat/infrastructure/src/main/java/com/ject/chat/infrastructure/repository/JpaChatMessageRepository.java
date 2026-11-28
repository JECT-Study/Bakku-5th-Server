package com.ject.chat.infrastructure.repository;

import com.ject.chat.domain.domain.message.ChatMessage;
import com.ject.chat.domain.domain.message.ChatMessageType;
import com.ject.chat.domain.repository.ChatMessageRepository;
import com.ject.chat.infrastructure.entity.ChatMessageEntity;
import com.ject.chat.infrastructure.entity.ChatMessageImageEntity;
import com.ject.chat.infrastructure.jpa.ChatMessageEntityRepository;
import com.ject.chat.infrastructure.jpa.ChatMessageImageEntityRepository;
import com.ject.chat.infrastructure.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaChatMessageRepository implements ChatMessageRepository {

    private final ChatMessageEntityRepository chatMessageEntityRepository;
    private final ChatMessageImageEntityRepository chatMessageImageEntityRepository;

    @Override
    @Transactional
    public ChatMessage save(ChatMessage chatMessage) {
        ChatMessageEntity savedMessage = chatMessageEntityRepository.save(ChatMessageMapper.toEntity(chatMessage));
        List<ChatMessageImageEntity> images = ChatMessageMapper.toImageEntities(savedMessage, chatMessage);
        if (!images.isEmpty()) {
            chatMessageImageEntityRepository.saveAll(images);
        }
        return toDomain(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> findRecent(Long chatRoomId, Long beforeMessageId, int limit) {
        Long before = beforeMessageId == null ? Long.MAX_VALUE : beforeMessageId;
        List<ChatMessageEntity> entities = chatMessageEntityRepository.findRecent(chatRoomId, before,
                Limit.of(limit));
        return toDomain(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> findAfter(Long chatRoomId, Long afterMessageId) {
        List<ChatMessageEntity> entities =
                chatMessageEntityRepository.findByChatRoomIdAndIdGreaterThanOrderByIdAsc(chatRoomId, afterMessageId);
        return toDomain(entities);
    }

    @Override
    public boolean existsByIdAndChatRoomId(Long messageId, Long chatRoomId) {
        return chatMessageEntityRepository.existsByIdAndChatRoomId(messageId, chatRoomId);
    }

    private ChatMessage toDomain(ChatMessageEntity entity) {
        List<ChatMessageImageEntity> images = entity.getType() == ChatMessageType.IMAGE
                ? chatMessageImageEntityRepository.findByMessageIdInOrderByMessageIdAscSortOrderAsc(
                        List.of(entity.getId()))
                : List.of();
        return ChatMessageMapper.toDomain(entity, images);
    }

    private List<ChatMessage> toDomain(List<ChatMessageEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        List<Long> messageIds = entities.stream().map(ChatMessageEntity::getId).toList();
        Map<Long, List<ChatMessageImageEntity>> imagesByMessageId =
                chatMessageImageEntityRepository.findByMessageIdInOrderByMessageIdAscSortOrderAsc(messageIds).stream()
                        .collect(Collectors.groupingBy(image -> image.getMessage().getId()));
        return entities.stream()
                .map(entity -> ChatMessageMapper.toDomain(entity,
                        imagesByMessageId.getOrDefault(entity.getId(), List.of())))
                .toList();
    }
}
