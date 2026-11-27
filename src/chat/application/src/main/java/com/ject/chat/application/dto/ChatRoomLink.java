package com.ject.chat.application.dto;

/**
 * (postId, requesterId) 조합의 교환·채팅방 연결 상태. exchange application이 신규 생성 여부를 판단하는 데 쓴다
 *
 * @param exchangeId 연결된 교환 ID. ABSENT면 null
 * @param chatRoomId 연결된 채팅방 ID. ABSENT·LEFT면 null
 * @param status     연결 상태
 */
public record ChatRoomLink(Long exchangeId, Long chatRoomId, ChatRoomLinkStatus status) {

    /**
     * 상태와 모순되는 ID 조합을 거부한다.
     *
     * @throws IllegalArgumentException ID 조합이 {@code status}와 일치하지 않을 때
     */
    public ChatRoomLink {
        if (status == null || !isConsistent(exchangeId, chatRoomId, status)) {
            throw new IllegalArgumentException("ChatRoomLink의 ID 조합이 상태와 일치하지 않습니다.");
        }
    }

    private static boolean isConsistent(Long exchangeId, Long chatRoomId, ChatRoomLinkStatus status) {
        return switch (status) {
            case ABSENT -> exchangeId == null && chatRoomId == null;
            case ACTIVE -> exchangeId != null && chatRoomId != null;
            case LEFT -> exchangeId != null && chatRoomId == null;
        };
    }
}
