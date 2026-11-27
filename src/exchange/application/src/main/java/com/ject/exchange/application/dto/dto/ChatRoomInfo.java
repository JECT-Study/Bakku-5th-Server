package com.ject.exchange.application.dto.dto;

/**
 * exchange 관점에서 본 (postId, requesterId) 조합의 교환·채팅방 연결 상태. chat BC의 타입에 의존하지 않는다
 *
 * @param exchangeId 연결된 교환 ID. ABSENT면 null
 * @param chatRoomId 연결된 채팅방 ID. ABSENT·LEFT면 null
 * @param status     연결 상태
 */
public record ChatRoomInfo(Long exchangeId, Long chatRoomId, Status status) {

    /**
     * 상태와 모순되는 ID 조합을 거부한다.
     *
     * @throws IllegalArgumentException ID 조합이 {@code status}와 일치하지 않을 때
     */
    public ChatRoomInfo {
        if (status == null || !isConsistent(exchangeId, chatRoomId, status)) {
            throw new IllegalArgumentException("ChatRoomInfo의 ID 조합이 상태와 일치하지 않습니다.");
        }
    }

    private static boolean isConsistent(Long exchangeId, Long chatRoomId, Status status) {
        return switch (status) {
            case ABSENT -> exchangeId == null && chatRoomId == null;
            case ACTIVE -> exchangeId != null && chatRoomId != null;
            case LEFT -> exchangeId != null && chatRoomId == null;
        };
    }

    public enum Status {
        /** 요청자가 이 게시글로 만든 방이 없음 */
        ABSENT,
        /** 요청자가 나가지 않아 다시 들어갈 수 있는 현재 방 */
        ACTIVE,
        /** 요청자가 나간 이력이 있는 방. 재요청을 허용하지 않는다 */
        LEFT
    }
}
