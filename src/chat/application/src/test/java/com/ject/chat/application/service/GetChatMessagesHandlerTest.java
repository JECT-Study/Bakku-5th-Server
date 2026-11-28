package com.ject.chat.application.service;

import com.ject.chat.application.dto.ChatMessagePage;
import com.ject.chat.application.dto.query.GetChatMessagesQuery;
import com.ject.chat.domain.domain.chatroom.ChatParticipant;
import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.domain.message.ChatMessage;
import com.ject.chat.domain.domain.message.ChatMessageType;
import com.ject.chat.domain.domain.message.TextContent;
import com.ject.chat.domain.exception.ChatErrorCode;
import com.ject.chat.domain.exception.ChatException;
import com.ject.chat.domain.repository.ChatMessageRepository;
import com.ject.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class GetChatMessagesHandlerTest {

    private static final Long ROOM_ID = 5L;
    private static final Long VIEWER_ID = 101L;
    private static final Instant NOW = Instant.parse("2026-11-28T06:00:00Z");

    private final FakeChatRoomRepository roomRepository = new FakeChatRoomRepository();
    private final FakeChatMessageRepository messageRepository = new FakeChatMessageRepository();
    private GetChatMessagesHandler handler;

    @BeforeEach
    void setUp() {
        roomRepository.room = room(null);
        messageRepository.messages.clear();
        handler = new GetChatMessagesHandler(roomRepository, messageRepository);
    }

    @Test
    @DisplayName("채팅방이 없으면 CHAT_001")
    void handle_missingRoom_throwsChat001() {
        roomRepository.room = null;

        ChatException exception = assertThrows(ChatException.class,
                () -> handler.handle(query(null, 30, null)));

        assertEquals(ChatErrorCode.CHAT_ROOM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("나간 참여자가 메시지 이력을 조회하면 CHAT_002")
    void handle_leftViewer_throwsChat002() {
        roomRepository.room = room(NOW);

        ChatException exception = assertThrows(ChatException.class,
                () -> handler.handle(query(null, 30, null)));

        assertEquals(ChatErrorCode.NOT_CHAT_PARTICIPANT, exception.getErrorCode());
    }

    @Test
    @DisplayName("첫 페이지는 최신순 size개와 다음 cursor를 반환한다")
    void handle_recentFirstPage_returnsDescendingPage() {
        messageRepository.messages.addAll(List.of(message(1L), message(2L), message(3L)));

        ChatMessagePage page = handler.handle(query(null, 2, null));

        assertEquals(List.of(3L, 2L), page.content().stream().map(result -> result.messageId()).toList());
        assertEquals(2L, page.nextCursorMessageId());
        assertTrue(page.hasNext());
        assertEquals(3, messageRepository.requestedLimit);
    }

    @Test
    @DisplayName("마지막 cursor 페이지는 이전 메시지만 반환하고 다음 cursor가 없다")
    void handle_recentLastPage_returnsOlderMessagesWithoutNext() {
        messageRepository.messages.addAll(List.of(message(1L), message(2L), message(3L)));

        ChatMessagePage page = handler.handle(query(2L, 2, null));

        assertEquals(List.of(1L), page.content().stream().map(result -> result.messageId()).toList());
        assertNull(page.nextCursorMessageId());
        assertFalse(page.hasNext());
    }

    @Test
    @DisplayName("after 조회는 이후 메시지만 오래된 순으로 반환한다")
    void handle_after_returnsAscendingMissedMessages() {
        messageRepository.messages.addAll(List.of(message(1L), message(2L), message(3L)));

        ChatMessagePage page = handler.handle(query(null, 1, 1L));

        assertEquals(List.of(2L, 3L), page.content().stream().map(result -> result.messageId()).toList());
        assertNull(page.nextCursorMessageId());
        assertFalse(page.hasNext());
    }

    @Test
    @DisplayName("다른 방의 cursor 기준 메시지는 CHAT_007")
    void handle_cursorOutsideRoom_throwsChat007() {
        messageRepository.messages.add(message(1L));

        ChatException exception = assertThrows(ChatException.class,
                () -> handler.handle(query(99L, 30, null)));

        assertEquals(ChatErrorCode.MESSAGE_NOT_IN_ROOM, exception.getErrorCode());
    }

    private static GetChatMessagesQuery query(Long cursor, int size, Long after) {
        return new GetChatMessagesQuery(ROOM_ID, VIEWER_ID, cursor, size, after);
    }

    private static ChatMessage message(Long id) {
        return ChatMessage.of(id, ROOM_ID, VIEWER_ID, ChatMessageType.TEXT, new TextContent("message-" + id, false),
                "client-" + id, NOW.plusSeconds(id), NOW.plusSeconds(id));
    }

    private static ChatRoom room(Instant viewerLeftAt) {
        return ChatRoom.of(ROOM_ID, 1L, 10L, VIEWER_ID, List.of(
                ChatParticipant.of(11L, ROOM_ID, VIEWER_ID, ChatParticipantRole.REQUESTER, viewerLeftAt, NOW, NOW),
                ChatParticipant.of(12L, ROOM_ID, 202L, ChatParticipantRole.OWNER, null, NOW, NOW)), NOW, NOW);
    }

    private static class FakeChatRoomRepository implements ChatRoomRepository {
        ChatRoom room;
        @Override public ChatRoom save(ChatRoom chatRoom) { return chatRoom; }
        @Override public Optional<ChatRoom> findById(Long id) { return Optional.ofNullable(room); }
        @Override public Optional<ChatRoom> findByPostIdAndRequesterId(Long postId, Long requesterId) { return Optional.empty(); }
        @Override public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) { return Optional.empty(); }
        @Override public int countActiveRequestsByPostId(Long postId) { return 0; }
    }

    private static class FakeChatMessageRepository implements ChatMessageRepository {
        final List<ChatMessage> messages = new ArrayList<>();
        int requestedLimit;
        @Override public ChatMessage save(ChatMessage message) { messages.add(message); return message; }
        @Override public List<ChatMessage> findRecent(Long roomId, Long before, int limit) {
            requestedLimit = limit;
            long boundary = before == null ? Long.MAX_VALUE : before;
            return messages.stream().filter(message -> message.getChatRoomId().equals(roomId))
                    .filter(message -> message.getId() < boundary)
                    .sorted(Comparator.comparing(ChatMessage::getId).reversed()).limit(limit).toList();
        }
        @Override public List<ChatMessage> findAfter(Long roomId, Long after) {
            return messages.stream().filter(message -> message.getChatRoomId().equals(roomId))
                    .filter(message -> message.getId() > after)
                    .sorted(Comparator.comparing(ChatMessage::getId)).toList();
        }
        @Override public boolean existsByIdAndChatRoomId(Long id, Long roomId) {
            return messages.stream().anyMatch(message -> message.getId().equals(id)
                    && message.getChatRoomId().equals(roomId));
        }
    }
}
