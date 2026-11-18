package com.ject.user.api.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class UserStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    static final Long MY_ID = 101L;
    static final String MY_NICKNAME = "쿠로미덕후";
    static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 27, 14, 30, 0, 0, KST);

    private UserStubData() {
    }
}
