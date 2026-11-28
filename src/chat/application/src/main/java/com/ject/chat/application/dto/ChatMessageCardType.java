package com.ject.chat.application.dto;

/**
 * CARD 메시지 종류. domain enum을 그대로 노출하지 않고 application 전용으로 옮긴다
 */
public enum ChatMessageCardType {
    EXCHANGE_ITEMS,
    APPOINTMENT_PROPOSAL,
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_CHANGE,
    CANCELLATION_REQUEST,
    EXCHANGE_CONFIRMATION,
    EXCHANGE_COMPLETED
}
