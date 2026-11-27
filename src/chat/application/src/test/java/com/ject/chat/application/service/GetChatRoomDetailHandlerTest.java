package com.ject.chat.application.service;

import com.ject.chat.application.dto.ChatAppointmentState;
import com.ject.chat.application.dto.ChatExchangeState;
import com.ject.chat.application.dto.ChatInputDisabledReason;
import com.ject.chat.application.dto.ChatMemberInfo;
import com.ject.chat.application.dto.ChatMemberRestriction;
import com.ject.chat.application.dto.ChatRoomAccessStatus;
import com.ject.chat.application.dto.ChatRoomDetailResult;
import com.ject.chat.application.dto.ChatRoomLeaveImpact;
import com.ject.chat.application.dto.query.GetChatRoomDetailQuery;
import com.ject.chat.application.port.ChatExchangePort;
import com.ject.chat.application.port.ChatMemberPort;
import com.ject.chat.domain.domain.chatroom.ChatParticipant;
import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.exception.ChatErrorCode;
import com.ject.chat.domain.exception.ChatException;
import com.ject.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class GetChatRoomDetailHandlerTest {

    private static final Long ROOM_ID = 5L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long VIEWER_ID = 101L;
    private static final Long COUNTERPART_ID = 202L;
    private static final Instant NOW = Instant.parse("2026-11-28T06:00:00Z");

    private final FakeChatRoomRepository repository = new FakeChatRoomRepository();
    private final FakeChatMemberPort memberPort = new FakeChatMemberPort();
    private final FakeChatExchangePort exchangePort = new FakeChatExchangePort();
    private GetChatRoomDetailHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetChatRoomDetailHandler(repository, memberPort, exchangePort);
        repository.room = room(null, null, false, 117L);
    }

    @Test
    @DisplayName("채팅방이 없으면 CHAT_001")
    void handle_missingRoom_throwsChat001() {
        repository.room = null;

        ChatException exception = assertThrows(ChatException.class,
                () -> handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID)));

        assertEquals(ChatErrorCode.CHAT_ROOM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("현재 참여자는 실제 상대 정보와 자기 알림·읽음 상태를 조회한다")
    void handle_activeParticipant_returnsActualDetail() {
        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ROOM_ID, result.chatRoomId());
        assertEquals(EXCHANGE_ID, result.exchangeId());
        assertEquals(COUNTERPART_ID, result.counterpart().userId());
        assertEquals("하치와레러버", result.counterpart().nickname());
        assertEquals(5, result.counterpart().completedCount());
        assertFalse(result.counterpart().left());
        assertFalse(result.notificationEnabled());
        assertEquals(117L, result.myLastReadMessageId());
        assertEquals(ChatRoomAccessStatus.OPEN, result.status());
        assertNull(result.inputDisabledReason());
    }

    @Test
    @DisplayName("차단과 다른 교환 완료가 겹치면 차단을 우선한다")
    void handle_blockedAndOtherCompleted_prioritizesBlocked() {
        memberPort.blocked = true;
        exchangePort.state = new ChatExchangeState(true, ChatAppointmentState.NONE);

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatRoomAccessStatus.READ_ONLY, result.status());
        assertEquals(ChatInputDisabledReason.BLOCKED, result.inputDisabledReason());
    }

    @Test
    @DisplayName("탈퇴·제한·퇴장·다른 교환 완료를 승인된 우선순위로 판정한다")
    void handle_overlappingDisabledReasons_usesApprovedPriority() {
        memberPort.counterpart = new ChatMemberInfo(COUNTERPART_ID, "탈퇴한 사용자", 5, true);
        memberPort.counterpartRestricted = true;
        memberPort.viewerRestricted = true;
        repository.room = room(null, NOW, true, null);
        exchangePort.state = new ChatExchangeState(true, ChatAppointmentState.NONE);

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatInputDisabledReason.COUNTERPART_WITHDRAWN, result.inputDisabledReason());
    }

    @Test
    @DisplayName("상대가 이용 제한 중이면 상대 제한 사유로 읽기 전용이다")
    void handle_counterpartRestricted_returnsCounterpartRestricted() {
        memberPort.counterpartRestricted = true;

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatInputDisabledReason.COUNTERPART_RESTRICTED, result.inputDisabledReason());
    }

    @Test
    @DisplayName("조회 회원이 이용 제한 중이면 회원 제한 사유로 읽기 전용이다")
    void handle_viewerRestricted_returnsMemberRestricted() {
        memberPort.viewerRestricted = true;

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatInputDisabledReason.MEMBER_RESTRICTED, result.inputDisabledReason());
    }

    @Test
    @DisplayName("상대가 나갔으면 상대 퇴장 사유로 읽기 전용이다")
    void handle_counterpartLeft_returnsCounterpartLeft() {
        repository.room = room(null, NOW, true, null);

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatInputDisabledReason.COUNTERPART_LEFT, result.inputDisabledReason());
    }

    @Test
    @DisplayName("다른 상대와 교환 완료된 게시글이면 다른 교환 완료 사유로 읽기 전용이다")
    void handle_otherExchangeCompleted_returnsOtherExchangeCompleted() {
        exchangePort.state = new ChatExchangeState(true, ChatAppointmentState.NONE);

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatInputDisabledReason.OTHER_EXCHANGE_COMPLETED, result.inputDisabledReason());
    }

    @Test
    @DisplayName("교환 확인 질문이 열린 뒤 나가기는 교환 완료 영향이다")
    void handle_confirmationOpened_returnsCompleteExchangeImpact() {
        exchangePort.state = new ChatExchangeState(false, ChatAppointmentState.CONFIRMATION_OPENED);

        ChatRoomDetailResult result = handler.handle(new GetChatRoomDetailQuery(ROOM_ID, VIEWER_ID));

        assertEquals(ChatRoomLeaveImpact.COMPLETE_EXCHANGE, result.leaveImpact());
    }

    private static ChatRoom room(Instant viewerLeftAt, Instant counterpartLeftAt,
                                 boolean notificationEnabled, Long lastReadMessageId) {
        return ChatRoom.of(ROOM_ID, EXCHANGE_ID, POST_ID, VIEWER_ID, java.util.List.of(
                ChatParticipant.of(11L, ROOM_ID, VIEWER_ID, ChatParticipantRole.REQUESTER, viewerLeftAt,
                        notificationEnabled, lastReadMessageId, NOW, NOW),
                ChatParticipant.of(12L, ROOM_ID, COUNTERPART_ID, ChatParticipantRole.OWNER, counterpartLeftAt,
                        true, null, NOW, NOW)), NOW, NOW);
    }

    private static class FakeChatRoomRepository implements ChatRoomRepository {
        ChatRoom room;
        @Override public ChatRoom save(ChatRoom chatRoom) { this.room = chatRoom; return chatRoom; }
        @Override public Optional<ChatRoom> findById(Long id) { return Optional.ofNullable(room); }
        @Override public Optional<ChatRoom> findByPostIdAndRequesterId(Long postId, Long requesterId) { return Optional.empty(); }
        @Override public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) { return Optional.empty(); }
        @Override public int countActiveRequestsByPostId(Long postId) { return 0; }
    }

    private static class FakeChatMemberPort implements ChatMemberPort {
        ChatMemberInfo counterpart = new ChatMemberInfo(COUNTERPART_ID, "하치와레러버", 5, false);
        boolean blocked;
        boolean viewerRestricted;
        boolean counterpartRestricted;
        @Override public ChatMemberInfo getSummary(Long memberId) { return counterpart; }
        @Override public boolean isBlocked(Long memberId, Long counterpartId) { return blocked; }
        @Override public ChatMemberRestriction getRestriction(Long memberId) {
            return new ChatMemberRestriction(memberId.equals(VIEWER_ID) ? viewerRestricted : counterpartRestricted);
        }
    }

    private static class FakeChatExchangePort implements ChatExchangePort {
        ChatExchangeState state = new ChatExchangeState(false, ChatAppointmentState.NONE);
        @Override public ChatExchangeState getExchangeState(Long exchangeId, Long viewerId) { return state; }
    }
}
