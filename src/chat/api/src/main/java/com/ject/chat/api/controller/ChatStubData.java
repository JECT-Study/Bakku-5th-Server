package com.ject.chat.api.controller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ChatStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    static final Long CHAT_ROOM_ID = 5L;
    static final Long EXCHANGE_ID = 1L;
    static final Long APPOINTMENT_ID = 3L;
    static final Long MY_USER_ID = 101L;
    static final Long COUNTERPART_ID = 202L;
    static final String COUNTERPART_NICKNAME = "하치와레러버";
    static final String CLIENT_MESSAGE_ID = "2f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f";

    static final Long LATEST_MESSAGE_ID = 121L;
    static final String LATEST_MESSAGE_TEXT = "내일 3시 홍대입구역 어떠세요?";
    static final OffsetDateTime LATEST_MESSAGE_SENT_AT = at(12, 14, 31);

    private ChatStubData() {
    }

    static OffsetDateTime at(int dayOfOctober, int hour, int minute) {
        return OffsetDateTime.of(2026, 10, dayOfOctober, hour, minute, 0, 0, KST);
    }
}
