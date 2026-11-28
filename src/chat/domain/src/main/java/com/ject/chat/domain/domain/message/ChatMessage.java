package com.ject.chat.domain.domain.message;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ChatMessage {

    private final Long id;
    private final Long chatRoomId;
    private final Long senderUserId;
    private final ChatMessageType type;
    private final ChatMessageContent content;
    private final String clientMessageId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ChatMessage(Long id, Long chatRoomId, Long senderUserId, ChatMessageType type, ChatMessageContent content,
                         String clientMessageId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderUserId = senderUserId;
        this.type = type;
        this.content = content;
        this.clientMessageId = clientMessageId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 참여자가 보낸 TEXT 메시지를 만든다
     */
    public static ChatMessage createText(Long chatRoomId, Long senderUserId, String clientMessageId,
                                          TextContent content) {
        validateUserMessage(chatRoomId, senderUserId, content);
        return new ChatMessage(null, chatRoomId, senderUserId, ChatMessageType.TEXT, content, clientMessageId, null,
                null);
    }

    /**
     * 참여자가 보낸 IMAGE 메시지를 만든다
     */
    public static ChatMessage createImage(Long chatRoomId, Long senderUserId, String clientMessageId,
                                           ImageContent content) {
        validateUserMessage(chatRoomId, senderUserId, content);
        return new ChatMessage(null, chatRoomId, senderUserId, ChatMessageType.IMAGE, content, clientMessageId, null,
                null);
    }

    /**
     * 서버가 만드는 SYSTEM 메시지를 만든다. sender와 clientMessageId는 가지지 않는다
     */
    public static ChatMessage createSystem(Long chatRoomId, SystemContent content) {
        validateServerMessage(chatRoomId, content);
        return new ChatMessage(null, chatRoomId, null, ChatMessageType.SYSTEM, content, null, null, null);
    }

    /**
     * 서버가 만드는 CARD 메시지를 만든다. sender와 clientMessageId는 가지지 않는다
     */
    public static ChatMessage createCard(Long chatRoomId, CardContent content) {
        validateServerMessage(chatRoomId, content);
        return new ChatMessage(null, chatRoomId, null, ChatMessageType.CARD, content, null, null, null);
    }

    /**
     * 영속 데이터에서 복원한다. 타입별 내용을 재검증해 조회 중 손상을 성공으로 숨기지 않는다
     */
    public static ChatMessage of(Long id, Long chatRoomId, Long senderUserId, ChatMessageType type,
                                  ChatMessageContent content, String clientMessageId, Instant createdAt,
                                  Instant updatedAt) {
        validateTypeContentMatch(type, content);
        if (type == ChatMessageType.SYSTEM || type == ChatMessageType.CARD) {
            validateServerMessage(chatRoomId, content);
            validateNoUserIdentity(senderUserId, clientMessageId);
        } else {
            validateUserMessage(chatRoomId, senderUserId, content);
        }
        return new ChatMessage(id, chatRoomId, senderUserId, type, content, clientMessageId, createdAt, updatedAt);
    }

    private static void validateUserMessage(Long chatRoomId, Long senderUserId, ChatMessageContent content) {
        if (chatRoomId == null || senderUserId == null || content == null) {
            throw new IllegalArgumentException("사용자 메시지는 채팅방 ID, 보낸 사람, 내용이 모두 필요합니다.");
        }
    }

    private static void validateServerMessage(Long chatRoomId, ChatMessageContent content) {
        if (chatRoomId == null || content == null) {
            throw new IllegalArgumentException("서버 메시지는 채팅방 ID와 내용이 필요합니다.");
        }
    }

    private static void validateNoUserIdentity(Long senderUserId, String clientMessageId) {
        if (senderUserId != null || clientMessageId != null) {
            throw new IllegalArgumentException("SYSTEM·CARD 메시지는 보낸 사람과 clientMessageId를 가질 수 없습니다.");
        }
    }

    private static void validateTypeContentMatch(ChatMessageType type, ChatMessageContent content) {
        if (type == null) {
            throw new IllegalArgumentException("메시지 타입이 필요합니다.");
        }
        boolean matches = switch (type) {
            case TEXT -> content instanceof TextContent;
            case IMAGE -> content instanceof ImageContent;
            case SYSTEM -> content instanceof SystemContent;
            case CARD -> content instanceof CardContent;
        };
        if (!matches) {
            throw new IllegalArgumentException("메시지 타입과 내용이 일치하지 않습니다.");
        }
    }
}
