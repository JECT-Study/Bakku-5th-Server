package com.ject.chat.domain.domain.message;

public enum CardType {

    /**
     * 교환 아이템 카드. targetId는 exchangeId
     */
    EXCHANGE_ITEMS,

    /**
     * 약속 제안 카드. targetId는 appointmentId
     */
    APPOINTMENT_PROPOSAL,

    /**
     * 약속 확정 카드. targetId는 appointmentId
     */
    APPOINTMENT_CONFIRMED,

    /**
     * 약속 변경 카드. targetId는 changeId
     */
    APPOINTMENT_CHANGE,

    /**
     * 약속 취소 요청 카드. targetId는 cancellationId
     */
    CANCELLATION_REQUEST,

    /**
     * 교환 확인 요청 카드. targetId는 exchangeId
     */
    EXCHANGE_CONFIRMATION,

    /**
     * 교환 완료 카드. targetId는 exchangeId
     */
    EXCHANGE_COMPLETED
}
