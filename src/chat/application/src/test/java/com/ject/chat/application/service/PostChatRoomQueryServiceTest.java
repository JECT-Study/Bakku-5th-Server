package com.ject.chat.application.service;

import com.ject.chat.application.dto.PostChatRoomInfo;
import com.ject.chat.domain.domain.chatroom.ChatParticipant;
import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
class PostChatRoomQueryServiceTest {

    private static final Long POST_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-11-27T03:00:00Z");

    @Test
    @DisplayName("현재 방 ID와 요청자가 남아 있는 방 수를 함께 반환한다")
    void getPostChatRoomInfo_activeRoom_returnsIdAndActiveCount() {
        FakeChatRoomRepository repository = new FakeChatRoomRepository();
        repository.add(room(1L, 101L, null));
        repository.add(room(2L, 102L, null));
        repository.add(room(3L, 103L, NOW));
        PostChatRoomQueryService service = new PostChatRoomQueryService(repository);

        PostChatRoomInfo info = service.getPostChatRoomInfo(101L, POST_ID);

        assertEquals(1L, info.chatRoomId());
        assertFalse(info.hasLeftChatRoom());
        assertEquals(2, info.requestCount());
    }

    @Test
    @DisplayName("나간 방이면 현재 방 없이 나간 이력과 유효 요청 수를 반환한다")
    void getPostChatRoomInfo_leftRoom_returnsLeftHistory() {
        FakeChatRoomRepository repository = new FakeChatRoomRepository();
        repository.add(room(1L, 101L, NOW));
        repository.add(room(2L, 102L, null));
        PostChatRoomQueryService service = new PostChatRoomQueryService(repository);

        PostChatRoomInfo info = service.getPostChatRoomInfo(101L, POST_ID);

        assertNull(info.chatRoomId());
        assertTrue(info.hasLeftChatRoom());
        assertEquals(1, info.requestCount());
    }

    private static ChatRoom room(Long id, Long requesterId, Instant leftAt) {
        Long ownerId = 900L;
        return ChatRoom.of(id, id + 100L, POST_ID, requesterId, List.of(
                ChatParticipant.of(id * 10, id, requesterId, ChatParticipantRole.REQUESTER, leftAt, NOW, NOW),
                ChatParticipant.of(id * 10 + 1, id, ownerId, ChatParticipantRole.OWNER, null, NOW, NOW)), NOW, NOW);
    }

    private static class FakeChatRoomRepository implements ChatRoomRepository {
        private final List<ChatRoom> rooms = new ArrayList<>();

        @Override public ChatRoom save(ChatRoom chatRoom) { rooms.add(chatRoom); return chatRoom; }
        @Override public Optional<ChatRoom> findById(Long id) { return rooms.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public Optional<ChatRoom> findByPostIdAndRequesterId(Long postId, Long requesterId) {
            return rooms.stream().filter(r -> r.getPostId().equals(postId) && r.getRequesterId().equals(requesterId)).findFirst();
        }
        @Override public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
            return rooms.stream().filter(r -> r.getExchangeId().equals(exchangeId)).map(ChatRoom::getId).findFirst();
        }
        @Override public int countActiveRequestsByPostId(Long postId) {
            return (int) rooms.stream().filter(r -> r.getPostId().equals(postId) && r.isActiveForRequester()).count();
        }
        void add(ChatRoom room) { rooms.add(room); }
    }
}
