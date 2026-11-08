package com.ject.user.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportErrorCodeTest {

    @Test
    @DisplayName("에러 코드와 HTTP 상태가 API 명세와 같다")
    void matchesSpec() {
        assertAll(
                () -> assertCode(ReportErrorCode.REPORT_TARGET_NOT_FOUND, "REPORT_001", 404),
                () -> assertCode(ReportErrorCode.ALREADY_REPORTED, "REPORT_002", 409),
                () -> assertCode(ReportErrorCode.ITEM_PROBLEM_NOT_ALLOWED, "REPORT_003", 400),
                () -> assertCode(ReportErrorCode.SELF_REPORT, "REPORT_004", 400));
    }

    private static void assertCode(ReportErrorCode errorCode, String code, int status) {
        assertEquals(code, errorCode.getCode());
        assertEquals(status, errorCode.getStatus());
    }
}
