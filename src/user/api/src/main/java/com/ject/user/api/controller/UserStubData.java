package com.ject.user.api.controller;

import com.ject.user.api.dto.response.LoginMemberResponse;
import com.ject.user.api.dto.response.TokenResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class UserStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    static final Long MY_ID = 101L;
    static final String MY_NICKNAME = "쿠로미덕후";
    static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 27, 14, 30, 0, 0, KST);

    private UserStubData() {
    }

    static LoginMemberResponse me() {
        return new LoginMemberResponse(MY_ID, MY_NICKNAME);
    }

    static TokenResponse token() {
        return new TokenResponse("eyJhbGciOiJIUzI1NiJ9.access", NOW.plusHours(1), "eyJhbGciOiJIUzI1NiJ9.refresh", me());
    }
}
