package com.ject.chat.domain.domain.chatroom;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class ChatRoom {

    private final Long id;
    private final Long exchangeId;
    private final Long postId;
    private final Long requesterId;
    private final List<ChatParticipant> participants;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ChatRoom(Long id, Long exchangeId, Long postId, Long requesterId, List<ChatParticipant> participants,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.exchangeId = exchangeId;
        this.postId = postId;
        this.requesterId = requesterId;
        this.participants = List.copyOf(participants);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 교환 요청 하나에 연결되는 1:1 채팅방을 만든다. 요청자·게시자 참여자를 함께 만들고 중복을 검증한다
     */
    public static ChatRoom create(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        validateRequiredIds(exchangeId, postId, requesterId, ownerId);
        if (requesterId.equals(ownerId)) {
            throw new IllegalArgumentException("요청자와 게시자는 서로 다른 회원이어야 합니다.");
        }
        List<ChatParticipant> participants = List.of(
                ChatParticipant.create(requesterId, ChatParticipantRole.REQUESTER),
                ChatParticipant.create(ownerId, ChatParticipantRole.OWNER)
        );
        validateDistinctParticipants(participants);
        return new ChatRoom(null, exchangeId, postId, requesterId, participants, null, null);
    }

    /**
     * 영속 데이터에서 복원한다
     */
    public static ChatRoom of(Long id, Long exchangeId, Long postId, Long requesterId, List<ChatParticipant> participants,
                               Instant createdAt, Instant updatedAt) {
        return new ChatRoom(id, exchangeId, postId, requesterId, participants, createdAt, updatedAt);
    }

    public ChatParticipant requesterParticipant() {
        return participants.stream()
                .filter(participant -> participant.getRole() == ChatParticipantRole.REQUESTER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("요청자 참여자가 없습니다."));
    }

    /**
     * 요청자가 나가지 않아 다시 들어갈 수 있는 현재 방인지 확인한다
     */
    public boolean isActiveForRequester() {
        return !requesterParticipant().hasLeft();
    }

    private static void validateRequiredIds(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        if (exchangeId == null || postId == null || requesterId == null || ownerId == null) {
            throw new IllegalArgumentException("채팅방 생성에는 교환·게시글·요청자·게시자 ID가 모두 필요합니다.");
        }
    }

    private static void validateDistinctParticipants(List<ChatParticipant> participants) {
        long distinctMembers = participants.stream().map(ChatParticipant::getMemberId).distinct().count();
        long distinctRoles = participants.stream().map(ChatParticipant::getRole).distinct().count();
        if (distinctMembers != participants.size() || distinctRoles != participants.size()) {
            throw new IllegalArgumentException("한 방에 같은 회원 또는 같은 역할이 중복될 수 없습니다.");
        }
    }
}
