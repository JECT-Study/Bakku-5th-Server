package com.ject.exchange.api.dto.enums;

public enum AppointmentStatus {
    PROPOSED,
    REJECTED,
    WITHDRAWN,
    // 출시 후 검토 (E-10-1, #117): 자동 만료
    // EXPIRED,
    AUTO_REJECTED,
    CONFIRMED,
    COMPLETED,
    NOT_MET,
    CANCELED
}
