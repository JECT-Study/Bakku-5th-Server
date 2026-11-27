package com.ject.chat.application.service;

import com.ject.chat.application.dto.ChatRoomLink;
import com.ject.chat.application.dto.ChatRoomLinkStatus;
import com.ject.chat.domain.domain.chatroom.ChatParticipant;
import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.repository.ChatRoomLockRepository;
import com.ject.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ChatRoomCommandServiceTest {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long CHAT_ROOM_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-11-27T03:00:00Z");

    private FakeChatRoomRepository chatRoomRepository;
    private FakeChatRoomLockRepository lockRepository;
    private ChatRoomCommandService service;

    @BeforeEach
    void setUp() {
        chatRoomRepository = new FakeChatRoomRepository();
        lockRepository = new FakeChatRoomLockRepository();
        service = new ChatRoomCommandService(chatRoomRepository, lockRepository);
    }

    @Test
    @DisplayName("기존 방 조회 전에 게시글과 요청자 조합 잠금을 획득한다")
    void lockAndFind_locksBeforeReading() {
        chatRoomRepository.onFind = () -> assertEquals(List.of("lock"), lockRepository.events);

        ChatRoomLink link = service.lockAndFind(POST_ID, REQUESTER_ID);

        assertEquals(ChatRoomLinkStatus.ABSENT, link.status());
        assertEquals(List.of("lock"), lockRepository.events);
    }

    @Test
    @DisplayName("요청자가 남아 있는 기존 방이면 교환과 방 ID를 반환한다")
    void lockAndFind_activeRoom_returnsExistingIds() {
        chatRoomRepository.preload(activeRoom());

        ChatRoomLink link = service.lockAndFind(POST_ID, REQUESTER_ID);

        assertEquals(new ChatRoomLink(EXCHANGE_ID, CHAT_ROOM_ID, ChatRoomLinkStatus.ACTIVE), link);
    }

    @Test
    @DisplayName("요청자가 나간 기존 방이면 교환 ID와 나간 상태를 반환한다")
    void lockAndFind_leftRoom_returnsLeft() {
        chatRoomRepository.preload(leftRoom());

        ChatRoomLink link = service.lockAndFind(POST_ID, REQUESTER_ID);

        assertEquals(new ChatRoomLink(EXCHANGE_ID, null, ChatRoomLinkStatus.LEFT), link);
    }

    @Test
    @DisplayName("신규 채팅방을 만들면 요청자와 게시자를 함께 저장하고 방 ID를 반환한다")
    void create_savesRoomWithTwoParticipants() {
        Long chatRoomId = service.create(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID);

        assertEquals(CHAT_ROOM_ID, chatRoomId);
        ChatRoom saved = chatRoomRepository.findById(CHAT_ROOM_ID).orElseThrow();
        assertEquals(2, saved.getParticipants().size());
        assertEquals(List.of(REQUESTER_ID, OWNER_ID), saved.getParticipants().stream()
                .map(ChatParticipant::getMemberId).toList());
    }

    private static ChatRoom activeRoom() {
        return restoredRoom(null);
    }

    private static ChatRoom leftRoom() {
        return restoredRoom(NOW);
    }

    private static ChatRoom restoredRoom(Instant requesterLeftAt) {
        return ChatRoom.of(CHAT_ROOM_ID, EXCHANGE_ID, POST_ID, REQUESTER_ID, List.of(
                ChatParticipant.of(11L, CHAT_ROOM_ID, REQUESTER_ID, ChatParticipantRole.REQUESTER,
                        requesterLeftAt, NOW, NOW),
                ChatParticipant.of(12L, CHAT_ROOM_ID, OWNER_ID, ChatParticipantRole.OWNER,
                        null, NOW, NOW)), NOW, NOW);
    }

    private static class FakeChatRoomLockRepository implements ChatRoomLockRepository {
        private final List<String> events = new java.util.ArrayList<>();

        @Override
        public void lock(Long postId, Long requesterId) {
            assertEquals(POST_ID, postId);
            assertEquals(REQUESTER_ID, requesterId);
            events.add("lock");
        }
    }

    private static class FakeChatRoomRepository implements ChatRoomRepository {
        private final Map<Long, ChatRoom> store = new HashMap<>();
        private Runnable onFind = () -> { };

        @Override
        public ChatRoom save(ChatRoom chatRoom) {
            ChatRoom saved = ChatRoom.of(CHAT_ROOM_ID, chatRoom.getExchangeId(), chatRoom.getPostId(),
                    chatRoom.getRequesterId(), List.of(
                            ChatParticipant.of(11L, CHAT_ROOM_ID, REQUESTER_ID, ChatParticipantRole.REQUESTER,
                                    null, NOW, NOW),
                            ChatParticipant.of(12L, CHAT_ROOM_ID, OWNER_ID, ChatParticipantRole.OWNER,
                                    null, NOW, NOW)), NOW, NOW);
            store.put(CHAT_ROOM_ID, saved);
            return saved;
        }

        @Override
        public Optional<ChatRoom> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<ChatRoom> findByPostIdAndRequesterId(Long postId, Long requesterId) {
            onFind.run();
            return store.values().stream()
                    .filter(room -> room.getPostId().equals(postId) && room.getRequesterId().equals(requesterId))
                    .findFirst();
        }

        @Override
        public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
            return store.values().stream().filter(room -> room.getExchangeId().equals(exchangeId))
                    .map(ChatRoom::getId).findFirst();
        }

        @Override
        public int countActiveRequestsByPostId(Long postId) {
            return (int) store.values().stream()
                    .filter(room -> room.getPostId().equals(postId) && room.isActiveForRequester()).count();
        }

        void preload(ChatRoom chatRoom) {
            store.put(chatRoom.getId(), chatRoom);
        }
    }
}
