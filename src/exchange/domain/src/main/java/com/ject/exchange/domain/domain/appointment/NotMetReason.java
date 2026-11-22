package com.ject.exchange.domain.domain.appointment;

/**
 * 못 만난 이유 (F-04-1)
 */
public enum NotMetReason {
    /**
     * 상대가 연락 없이 안 나왔어요
     */
    NO_SHOW,
    /**
     * 제가 못 갔어요
     */
    SELF_ABSENT,
    /**
     * 물품이 달라요
     */
    ITEM_MISMATCH,
    /**
     * 금품을 요구했어요
     */
    MONEY_DEMANDED,
    /**
     * 서로 약속을 취소했어요
     */
    MUTUAL_CANCEL,
    /**
     * 직접 입력
     */
    OTHER
}
