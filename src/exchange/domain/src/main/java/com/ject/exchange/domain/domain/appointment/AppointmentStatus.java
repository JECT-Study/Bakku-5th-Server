package com.ject.exchange.domain.domain.appointment;

public enum AppointmentStatus {
    /**
     * 제안됨. 상대의 수락을 기다리는 상태
     */
    PROPOSED,
    /**
     * 상대가 거절함
     */
    REJECTED,
    /**
     * 제안자가 철회함
     */
    WITHDRAWN,
    /**
     * 응답 없이 만료됨
     */
    EXPIRED,
    /**
     * 같은 글의 다른 약속이 확정되어 자동 거절됨
     */
    AUTO_REJECTED,
    /**
     * 확정됨
     */
    CONFIRMED,
    /**
     * 교환 완료
     */
    COMPLETED,
    /**
     * 만나지 못함
     */
    NOT_MET,
    /**
     * 확정 후 취소됨
     */
    CANCELED
}
