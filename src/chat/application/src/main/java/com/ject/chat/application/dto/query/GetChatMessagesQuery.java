package com.ject.chat.application.dto.query;

import com.ject.chat.application.dto.ChatMessagePage;
import com.ject.common.core.cqrs.query.Query;
import lombok.Builder;

/**
 * CH-3 메시지 이력 조회. cursor(과거 조회)와 after(연결 복구 조회)는 함께 쓸 수 없다
 *
 * @param cursorMessageId 이전 페이지의 마지막 메시지 id. 첫 페이지면 null
 * @param size            한 페이지 크기 (1~100). after 모드에서는 무시
 * @param afterMessageId  이 메시지 id보다 큰 메시지만 오름차순으로 조회. null이면 cursor 모드
 */
public record GetChatMessagesQuery(
        Long chatRoomId,
        Long viewerId,
        Long cursorMessageId,
        Integer size,
        Long afterMessageId
) implements Query<ChatMessagePage> {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    // API가 이미 거르는 입력(cursor 형식, cursor·after 동시 입력, size 범위)도 application 경계에서 다시 방어한다.
    // 이 경계를 넘는 입력은 호출 코드의 프로그래밍 오류로 보고 IllegalArgumentException으로 거부한다
    @Builder
    public GetChatMessagesQuery {
        if (chatRoomId == null || viewerId == null) {
            throw new IllegalArgumentException("채팅방 ID와 조회 회원 ID가 필요합니다.");
        }
        if (size == null || size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("size는 " + MIN_SIZE + "~" + MAX_SIZE + " 사이여야 합니다.");
        }
        if (cursorMessageId != null && afterMessageId != null) {
            throw new IllegalArgumentException("cursor와 after는 함께 쓸 수 없습니다.");
        }
        if (cursorMessageId != null && cursorMessageId <= 0) {
            throw new IllegalArgumentException("cursor는 양수여야 합니다.");
        }
        if (afterMessageId != null && afterMessageId <= 0) {
            throw new IllegalArgumentException("after는 양수여야 합니다.");
        }
    }
}
