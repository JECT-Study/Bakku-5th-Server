package com.ject.chat.domain.domain.chatroom;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ChatParticipant {

    private final Long id;
    private final Long chatRoomId;
    private final Long memberId;
    private final ChatParticipantRole role;
    private final Instant leftAt;
    private final boolean notificationEnabled;
    private final Long lastReadMessageId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ChatParticipant(Long id, Long chatRoomId, Long memberId, ChatParticipantRole role, Instant leftAt,
                             boolean notificationEnabled, Long lastReadMessageId,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.role = role;
        this.leftAt = leftAt;
        this.notificationEnabled = notificationEnabled;
        this.lastReadMessageId = lastReadMessageId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 채팅방에 들어가는 참여자 한 명을 요청자 또는 게시자 역할로 만든다. 알림은 기본 켜짐, 마지막으로 읽은 메시지는 없음이다
     */
    public static ChatParticipant create(Long memberId, ChatParticipantRole role) {
        if (memberId == null || role == null) {
            throw new IllegalArgumentException("참여자의 회원 ID와 역할은 필수입니다.");
        }
        return new ChatParticipant(null, null, memberId, role, null, true, null, null, null);
    }

    /**
     * 영속 데이터에서 복원한다 (#169 이전 호환: 개인 방 상태는 기본값으로 채운다)
     */
    public static ChatParticipant of(Long id, Long chatRoomId, Long memberId, ChatParticipantRole role, Instant leftAt,
                                      Instant createdAt, Instant updatedAt) {
        return new ChatParticipant(id, chatRoomId, memberId, role, leftAt, true, null, createdAt, updatedAt);
    }

    /**
     * 영속 데이터에서 복원한다 (알림 설정 · 마지막으로 읽은 메시지 ID 포함)
     */
    public static ChatParticipant of(Long id, Long chatRoomId, Long memberId, ChatParticipantRole role, Instant leftAt,
                                      boolean notificationEnabled, Long lastReadMessageId,
                                      Instant createdAt, Instant updatedAt) {
        return new ChatParticipant(id, chatRoomId, memberId, role, leftAt, notificationEnabled, lastReadMessageId,
                createdAt, updatedAt);
    }

    public boolean hasLeft() {
        return leftAt != null;
    }
}
