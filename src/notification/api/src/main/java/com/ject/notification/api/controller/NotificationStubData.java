package com.ject.notification.api.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class NotificationStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    static final OffsetDateTime NOW = OffsetDateTime.of(2026, 12, 1, 9, 0, 0, 0, KST);

    private NotificationStubData() {
    }
}
