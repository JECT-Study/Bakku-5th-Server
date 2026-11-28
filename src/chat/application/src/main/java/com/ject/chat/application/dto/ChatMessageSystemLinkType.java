package com.ject.chat.application.dto;

/**
 * SYSTEM 메시지 [보기] 버튼 대상 종류. domain enum을 그대로 노출하지 않고 application 전용으로 옮긴다
 */
public enum ChatMessageSystemLinkType {
    POST,
    EXCHANGE_ITEM
}
