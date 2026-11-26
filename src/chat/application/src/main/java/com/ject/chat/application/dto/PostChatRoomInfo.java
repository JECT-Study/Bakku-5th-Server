package com.ject.chat.application.dto;

/**
 * 회원과 게시글 하나의 채팅 상태를 한 조회 시점 기준으로 담은 결과.
 *
 * @param chatRoomId      회원이 이 게시글로 다시 들어갈 수 있는 현재 채팅방 ID. 요청한 적이 없거나 나간 이력이 있으면 null
 * @param hasLeftChatRoom 회원이 이 게시글로 만든 방에서 한 번이라도 나간 적이 있으면 true. 나간 방에는 다시 들어가거나 같은 게시글로 재요청할 수 없다
 * @param requestCount    이 게시글로 열린 방 중 요청자가 나가지 않은 방 수. 0 이상이다
 */
public record PostChatRoomInfo(Long chatRoomId, boolean hasLeftChatRoom, int requestCount) {

    /**
     * 모순 상태와 음수 요청 수를 거부한다.
     *
     * @throws IllegalArgumentException {@code hasLeftChatRoom}이 true인데 {@code chatRoomId}가 있거나, {@code requestCount}가 음수일 때
     */
    public PostChatRoomInfo {
        if (hasLeftChatRoom && chatRoomId != null) {
            throw new IllegalArgumentException("나간 이력이 있는 방은 현재 채팅방 ID를 가질 수 없습니다.");
        }
        if (requestCount < 0) {
            throw new IllegalArgumentException("요청 수는 음수일 수 없습니다.");
        }
    }
}
