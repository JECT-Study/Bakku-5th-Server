package com.ject.exchange.domain.domain.exchange;

public enum ExchangeStatus {
    /**
     * 교환이 진행 중인 상태
     */
    ACTIVE,
    /**
     * 교환이 완료된 상태
     */
    COMPLETED,
    /**
     * 교환이 닫힌 상태
     */
    CLOSED,
    /**
     * 교환이 남은 상태
     */
    LEFT
}
