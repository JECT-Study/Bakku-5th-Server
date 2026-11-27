package com.ject.chat.domain.domain.chatroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ChatRoomTest {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Instant NOW = Instant.parse("2026-11-27T03:00:00Z");

    @Test
    @DisplayName("채팅방을 만들면 요청자와 게시자가 서로 다른 역할로 참여한다")
    void create_addsRequesterAndOwner() {
        ChatRoom chatRoom = ChatRoom.create(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID);

        assertEquals(EXCHANGE_ID, chatRoom.getExchangeId());
        assertEquals(POST_ID, chatRoom.getPostId());
        assertEquals(REQUESTER_ID, chatRoom.getRequesterId());
        assertEquals(List.of(REQUESTER_ID, OWNER_ID), chatRoom.getParticipants().stream()
                .map(ChatParticipant::getMemberId).toList());
        assertEquals(List.of(ChatParticipantRole.REQUESTER, ChatParticipantRole.OWNER), chatRoom.getParticipants().stream()
                .map(ChatParticipant::getRole).toList());
        assertTrue(chatRoom.isActiveForRequester());
    }

    @Test
    @DisplayName("채팅방 생성 필수 ID가 없으면 명시적으로 실패한다")
    void create_missingRequiredId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ChatRoom.create(null, POST_ID, REQUESTER_ID, OWNER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRoom.create(EXCHANGE_ID, null, REQUESTER_ID, OWNER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRoom.create(EXCHANGE_ID, POST_ID, null, OWNER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRoom.create(EXCHANGE_ID, POST_ID, REQUESTER_ID, null));
    }

    @Test
    @DisplayName("요청자와 게시자가 같으면 1대1 채팅방을 만들 수 없다")
    void create_sameMember_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ChatRoom.create(EXCHANGE_ID, POST_ID, REQUESTER_ID, REQUESTER_ID));
    }

    @Test
    @DisplayName("복원한 요청자 참여자의 나간 시각으로 현재 방 여부를 판정한다")
    void restore_leftRequester_isNotActive() {
        ChatParticipant requester = ChatParticipant.of(11L, 5L, REQUESTER_ID,
                ChatParticipantRole.REQUESTER, NOW, NOW, NOW);
        ChatParticipant owner = ChatParticipant.of(12L, 5L, OWNER_ID,
                ChatParticipantRole.OWNER, null, NOW, NOW);

        ChatRoom chatRoom = ChatRoom.of(5L, EXCHANGE_ID, POST_ID, REQUESTER_ID,
                List.of(requester, owner), NOW, NOW);

        assertTrue(requester.hasLeft());
        assertFalse(chatRoom.isActiveForRequester());
    }
}
