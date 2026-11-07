package com.ject.post.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PostErrorCodeTest {

    @Test
    @DisplayName("에러 코드와 HTTP 상태가 API 명세와 같다")
    void matchesSpec() {
        assertAll(
                () -> assertCode(PostErrorCode.POST_NOT_FOUND, "POST_001", 404),
                () -> assertCode(PostErrorCode.NOT_POST_AUTHOR, "POST_002", 403),
                () -> assertCode(PostErrorCode.POST_RESERVED, "POST_003", 409),
                () -> assertCode(PostErrorCode.POST_COMPLETED, "POST_004", 409),
                () -> assertCode(PostErrorCode.INVALID_IMAGE_KEY, "POST_005", 400),
                () -> assertCode(PostErrorCode.RESTRICTED_MEMBER, "POST_006", 403));
    }

    private static void assertCode(PostErrorCode errorCode, String code, int status) {
        assertEquals(code, errorCode.getCode());
        assertEquals(status, errorCode.getStatus());
    }
}
