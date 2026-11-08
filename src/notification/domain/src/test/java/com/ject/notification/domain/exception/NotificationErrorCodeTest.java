package com.ject.notification.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationErrorCodeTest {

    @Test
    @DisplayName("에러 코드와 HTTP 상태가 API 명세와 같다")
    void matchesSpec() {
        assertEquals("NOTIFICATION_001", NotificationErrorCode.NOTIFICATION_NOT_FOUND.getCode());
        assertEquals(404, NotificationErrorCode.NOTIFICATION_NOT_FOUND.getStatus());
    }
}
