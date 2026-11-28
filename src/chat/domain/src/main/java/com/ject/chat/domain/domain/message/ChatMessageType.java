package com.ject.chat.domain.domain.message;

public enum ChatMessageType {

    /**
     * 참여자가 보낸 텍스트 메시지
     */
    TEXT,

    /**
     * 참여자가 보낸 사진 메시지
     */
    IMAGE,

    /**
     * 서버가 만드는 시스템 안내 메시지
     */
    SYSTEM,

    /**
     * 서버가 만드는 카드 메시지 (약속·교환 진행 상태)
     */
    CARD
}
