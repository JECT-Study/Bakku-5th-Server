package com.ject.user.application;

import com.ject.user.application.dto.SignupIdentity;
import com.ject.user.application.dto.SignupToken;
import com.ject.user.application.exception.InvalidSignupTokenException;
import com.ject.user.application.port.SignupTokenProvider;
import com.ject.user.domain.domain.SocialProvider;

import java.time.Instant;

/**
 * 테스트용 가입 토큰: "signup:{provider}:{해시}" 형식만 통과시킨다.
 */
class FakeSignupTokenProvider implements SignupTokenProvider {

    static final Instant EXPIRES_AT = Instant.parse("2026-11-22T03:00:00Z");
    private static final String PREFIX = "signup:";

    @Override
    public SignupToken issue(SignupIdentity identity) {
        return new SignupToken(PREFIX + identity.provider() + ":" + identity.socialIdHash(), EXPIRES_AT);
    }

    @Override
    public SignupIdentity verify(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            throw new InvalidSignupTokenException();
        }
        String[] parts = token.substring(PREFIX.length()).split(":", 2);
        return new SignupIdentity(SocialProvider.valueOf(parts[0]), parts[1]);
    }
}
