package com.ject.chat.infrastructure.mapper;

import com.ject.chat.domain.domain.message.CardContent;
import com.ject.chat.domain.domain.message.ChatMessage;
import com.ject.chat.domain.domain.message.ChatMessageContent;
import com.ject.chat.domain.domain.message.ImageContent;
import com.ject.chat.domain.domain.message.SystemContent;
import com.ject.chat.domain.domain.message.TextContent;
import com.ject.chat.infrastructure.entity.ChatMessageEntity;
import com.ject.chat.infrastructure.entity.ChatMessageImageEntity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class ChatMessageMapper {

    public static ChatMessageEntity toEntity(ChatMessage chatMessage) {
        ChatMessageEntity.ChatMessageEntityBuilder builder = ChatMessageEntity.builder()
                .id(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoomId())
                .senderUserId(chatMessage.getSenderUserId())
                .type(chatMessage.getType())
                .clientMessageId(chatMessage.getClientMessageId());
        switch (chatMessage.getContent()) {
            case TextContent text -> builder.textContent(text.text())
                    .externalTradeDetected(text.externalTradeDetected());
            case ImageContent ignored -> {
                // 이미지 목록은 별도 자식 행(ChatMessageImageEntity)에 저장한다
            }
            case SystemContent system -> builder.systemCode(system.code())
                    .systemText(system.text())
                    .systemLinkType(system.linkType())
                    .systemLinkId(system.linkId());
            case CardContent card -> builder.cardType(card.cardType()).cardTargetId(card.targetId());
        }
        return builder.build();
    }

    /**
     * IMAGE 메시지의 저장 대상 자식 행. IMAGE가 아니면 빈 목록
     */
    public static List<ChatMessageImageEntity> toImageEntities(ChatMessageEntity messageEntity,
                                                                ChatMessage chatMessage) {
        if (!(chatMessage.getContent() instanceof ImageContent imageContent)) {
            return List.of();
        }
        List<String> imageUrls = imageContent.imageUrls();
        return IntStream.range(0, imageUrls.size())
                .mapToObj(index -> ChatMessageImageEntity.builder()
                        .message(messageEntity)
                        .imageUrl(imageUrls.get(index))
                        .sortOrder(index + 1)
                        .build())
                .toList();
    }

    public static ChatMessage toDomain(ChatMessageEntity entity, List<ChatMessageImageEntity> images) {
        ChatMessageContent content = toContent(entity, images);
        return ChatMessage.of(entity.getId(), entity.getChatRoomId(), entity.getSenderUserId(), entity.getType(),
                content, entity.getClientMessageId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private static ChatMessageContent toContent(ChatMessageEntity entity, List<ChatMessageImageEntity> images) {
        return switch (entity.getType()) {
            case TEXT -> new TextContent(entity.getTextContent(),
                    Boolean.TRUE.equals(entity.getExternalTradeDetected()));
            case IMAGE -> new ImageContent(images.stream()
                    .sorted(Comparator.comparingInt(ChatMessageImageEntity::getSortOrder))
                    .map(ChatMessageImageEntity::getImageUrl)
                    .toList());
            case SYSTEM -> new SystemContent(entity.getSystemCode(), entity.getSystemText(),
                    entity.getSystemLinkType(), entity.getSystemLinkId());
            case CARD -> new CardContent(entity.getCardType(), entity.getCardTargetId());
        };
    }
}
