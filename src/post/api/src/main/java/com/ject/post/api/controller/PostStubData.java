package com.ject.post.api.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class PostStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    static final Long POST_ID = 10L;
    static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 11, 2, 14, 30, 0, 0, KST);
    static final OffsetDateTime NOW_EXCHANGE_ENDS_AT = CREATED_AT.plusMinutes(60);

    private PostStubData() {
    }
}
