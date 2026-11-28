package com.ject.chat.application.service;

import com.ject.chat.application.dto.CardMessageContentResult;
import com.ject.chat.application.dto.ChatMessageCardType;
import com.ject.chat.application.dto.ChatMessageContentResult;
import com.ject.chat.application.dto.ChatMessagePage;
import com.ject.chat.application.dto.ChatMessageResult;
import com.ject.chat.application.dto.ChatMessageSystemCode;
import com.ject.chat.application.dto.ChatMessageSystemLinkType;
import com.ject.chat.application.dto.ChatMessageType;
import com.ject.chat.application.dto.ImageMessageContentResult;
import com.ject.chat.application.dto.SystemMessageContentResult;
import com.ject.chat.application.dto.TextMessageContentResult;
import com.ject.chat.application.dto.query.GetChatMessagesQuery;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.domain.message.CardContent;
import com.ject.chat.domain.domain.message.ChatMessage;
import com.ject.chat.domain.domain.message.ChatMessageContent;
import com.ject.chat.domain.domain.message.ImageContent;
import com.ject.chat.domain.domain.message.SystemContent;
import com.ject.chat.domain.domain.message.TextContent;
import com.ject.chat.domain.exception.ChatErrorCode;
import com.ject.chat.domain.exception.ChatException;
import com.ject.chat.domain.repository.ChatMessageRepository;
import com.ject.chat.domain.repository.ChatRoomRepository;
import com.ject.common.core.cqrs.query.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetChatMessagesHandler implements QueryHandler<GetChatMessagesQuery, ChatMessagePage> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional(readOnly = true)
    public ChatMessagePage handle(GetChatMessagesQuery query) {
        ChatRoom chatRoom = chatRoomRepository.findById(query.chatRoomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoom.currentParticipantOf(query.viewerId());

        if (query.afterMessageId() != null) {
            return handleAfter(query);
        }
        return handleRecent(query);
    }

    private ChatMessagePage handleAfter(GetChatMessagesQuery query) {
        validateMessageInRoom(query.chatRoomId(), query.afterMessageId());
        List<ChatMessage> messages = chatMessageRepository.findAfter(query.chatRoomId(), query.afterMessageId());
        return new ChatMessagePage(toResults(messages), null, false);
    }

    private ChatMessagePage handleRecent(GetChatMessagesQuery query) {
        if (query.cursorMessageId() != null) {
            validateMessageInRoom(query.chatRoomId(), query.cursorMessageId());
        }
        List<ChatMessage> messages = chatMessageRepository.findRecent(query.chatRoomId(), query.cursorMessageId(),
                query.size() + 1);
        boolean hasNext = messages.size() > query.size();
        List<ChatMessage> page = hasNext ? messages.subList(0, query.size()) : messages;
        Long nextCursorMessageId = hasNext ? page.get(page.size() - 1).getId() : null;
        return new ChatMessagePage(toResults(page), nextCursorMessageId, hasNext);
    }

    private void validateMessageInRoom(Long chatRoomId, Long messageId) {
        if (!chatMessageRepository.existsByIdAndChatRoomId(messageId, chatRoomId)) {
            throw new ChatException(ChatErrorCode.MESSAGE_NOT_IN_ROOM);
        }
    }

    private static List<ChatMessageResult> toResults(List<ChatMessage> messages) {
        return messages.stream().map(GetChatMessagesHandler::toResult).toList();
    }

    private static ChatMessageResult toResult(ChatMessage message) {
        return new ChatMessageResult(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderUserId(),
                mapEnum(message.getType(), ChatMessageType.class),
                toContentResult(message.getContent()),
                message.getClientMessageId(),
                message.getCreatedAt()
        );
    }

    private static ChatMessageContentResult toContentResult(ChatMessageContent content) {
        return switch (content) {
            case TextContent text -> new TextMessageContentResult(text.text(), text.externalTradeDetected());
            case ImageContent image -> new ImageMessageContentResult(image.imageUrls());
            case SystemContent system -> new SystemMessageContentResult(
                    mapEnum(system.code(), ChatMessageSystemCode.class),
                    system.text(),
                    mapEnum(system.linkType(), ChatMessageSystemLinkType.class),
                    system.linkId());
            case CardContent card -> new CardMessageContentResult(
                    mapEnum(card.cardType(), ChatMessageCardType.class), card.targetId());
        };
    }

    private static <E extends Enum<E>> E mapEnum(Enum<?> source, Class<E> target) {
        return source == null ? null : Enum.valueOf(target, source.name());
    }
}
