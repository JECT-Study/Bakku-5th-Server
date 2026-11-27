package com.ject.chat.domain.domain.chatroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.ject.chat.domain.exception.ChatErrorCode;
import com.ject.chat.domain.exception.ChatException;
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

    @Test
    @DisplayName("새 참여자는 알림이 켜져 있고 마지막 읽은 메시지가 없다")
    void createParticipant_defaultsPersonalRoomState() {
        ChatParticipant participant = ChatParticipant.create(REQUESTER_ID, ChatParticipantRole.REQUESTER);

        assertTrue(participant.isNotificationEnabled());
        assertNull(participant.getLastReadMessageId());
    }

    @Test
    @DisplayName("참여자를 복원하면 알림 설정과 마지막 읽은 메시지를 보존한다")
    void restoreParticipant_preservesPersonalRoomState() {
        ChatParticipant participant = ChatParticipant.of(11L, 5L, REQUESTER_ID,
                ChatParticipantRole.REQUESTER, null, false, 117L, NOW, NOW);

        assertFalse(participant.isNotificationEnabled());
        assertEquals(117L, participant.getLastReadMessageId());
    }

    @Test
    @DisplayName("나간 회원은 현재 참여자로 채팅방을 조회할 수 없다")
    void currentParticipant_leftMember_throwsNotParticipant() {
        ChatRoom chatRoom = restoredRoom(NOW, null);

        ChatException exception = assertThrows(ChatException.class,
                () -> chatRoom.currentParticipantOf(REQUESTER_ID));

        assertEquals(ChatErrorCode.NOT_CHAT_PARTICIPANT, exception.getErrorCode());
    }

    @Test
    @DisplayName("상대가 나갔어도 현재 참여자는 상대 상태를 조회할 수 있다")
    void counterpart_leftMember_isReturned() {
        ChatRoom chatRoom = restoredRoom(null, NOW);

        ChatParticipant counterpart = chatRoom.counterpartOf(REQUESTER_ID);

        assertEquals(OWNER_ID, counterpart.getMemberId());
        assertTrue(counterpart.hasLeft());
    }

    private static ChatRoom restoredRoom(Instant requesterLeftAt, Instant ownerLeftAt) {
        return ChatRoom.of(5L, EXCHANGE_ID, POST_ID, REQUESTER_ID, List.of(
                ChatParticipant.of(11L, 5L, REQUESTER_ID, ChatParticipantRole.REQUESTER,
                        requesterLeftAt, true, null, NOW, NOW),
                ChatParticipant.of(12L, 5L, OWNER_ID, ChatParticipantRole.OWNER,
                        ownerLeftAt, true, null, NOW, NOW)), NOW, NOW);
    }
}
