package com.ject.user.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserErrorCodeTest {

    @Test
    @DisplayName("에러 코드와 HTTP 상태가 API 명세와 같다")
    void matchesSpec() {
        assertAll(
                () -> assertCode(UserErrorCode.SOCIAL_TOKEN_INVALID, "MEMBER_001", 401),
                () -> assertCode(UserErrorCode.REJOIN_NOT_AVAILABLE, "MEMBER_002", 403),
                () -> assertCode(UserErrorCode.MEMBER_BANNED, "MEMBER_003", 403),
                () -> assertCode(UserErrorCode.SIGNUP_TOKEN_INVALID, "MEMBER_010", 401),
                () -> assertCode(UserErrorCode.REQUIRED_TERMS_NOT_AGREED, "MEMBER_011", 400),
                () -> assertCode(UserErrorCode.TERMS_VERSION_OUTDATED, "MEMBER_012", 400),
                () -> assertCode(UserErrorCode.UNDER_AGE, "MEMBER_013", 400),
                () -> assertCode(UserErrorCode.ALREADY_SIGNED_UP, "MEMBER_014", 409),
                () -> assertCode(UserErrorCode.INVALID_NICKNAME, "MEMBER_101", 400),
                () -> assertCode(UserErrorCode.DUPLICATE_NICKNAME, "MEMBER_102", 409),
                () -> assertCode(UserErrorCode.NICKNAME_CHANGE_TOO_SOON, "MEMBER_103", 409),
                () -> assertCode(UserErrorCode.SAME_NICKNAME, "MEMBER_104", 400),
                () -> assertCode(UserErrorCode.MEMBER_NOT_FOUND, "MEMBER_201", 404),
                () -> assertCode(UserErrorCode.MEMBER_WITHDRAWN, "MEMBER_202", 404),
                () -> assertCode(UserErrorCode.BLOCKED_MEMBER, "MEMBER_203", 403),
                () -> assertCode(UserErrorCode.SELF_BLOCK, "MEMBER_401", 400),
                () -> assertCode(UserErrorCode.WISHLIST_KEYWORD_LIMIT, "MEMBER_501", 409),
                () -> assertCode(UserErrorCode.DUPLICATE_WISHLIST_KEYWORD, "MEMBER_502", 409),
                () -> assertCode(UserErrorCode.WISHLIST_KEYWORD_NOT_FOUND, "MEMBER_503", 404));
    }

    @Test
    @DisplayName("에러 코드는 서로 겹치지 않는다")
    void codesAreUnique() {
        long distinct = Arrays.stream(UserErrorCode.values()).map(UserErrorCode::getCode).distinct().count();

        assertEquals(UserErrorCode.values().length, distinct);
    }

    private static void assertCode(UserErrorCode errorCode, String code, int status) {
        assertEquals(code, errorCode.getCode());
        assertEquals(status, errorCode.getStatus());
    }
}
