package com.ject.user.application;

import com.ject.user.application.dto.AccessToken;
import com.ject.user.application.exception.InvalidAccessTokenException;
import com.ject.user.application.port.AccessTokenProvider;

import java.time.Instant;

/**
 * 테스트용 토큰: "token-{회원 id}" 형식만 통과시킨다.
 */
class FakeAccessTokenProvider implements AccessTokenProvider {

    private static final String PREFIX = "token-";
    static final Instant EXPIRES_AT = Instant.parse("2026-11-12T04:00:00Z");

    @Override
    public AccessToken issue(Long memberId) {
        return new AccessToken(PREFIX + memberId, EXPIRES_AT);
    }

    @Override
    public Long verify(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            throw InvalidAccessTokenException.invalid();
        }
        return Long.valueOf(token.substring(PREFIX.length()));
    }
}
