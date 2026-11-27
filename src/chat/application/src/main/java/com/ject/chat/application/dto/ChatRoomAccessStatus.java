package com.ject.chat.application.dto;

/**
 * 채팅방 상세 조회 시점의 입력 가능 상태
 */
public enum ChatRoomAccessStatus {
    /** 입력창을 보여준다 */
    OPEN,
    /** 방과 이전 대화는 보이지만 입력창은 보여주지 않는다 */
    READ_ONLY
}
