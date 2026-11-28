package com.ject.chat.application.dto;

/**
 * CH-3 메시지 조회 결과의 타입. domain enum을 그대로 노출하지 않고 application 전용으로 옮긴다
 */
public enum ChatMessageType {
    TEXT,
    IMAGE,
    SYSTEM,
    CARD
}
