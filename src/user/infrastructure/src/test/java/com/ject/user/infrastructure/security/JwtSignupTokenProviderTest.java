package com.ject.user.infrastructure.security;

import com.ject.user.application.dto.SignupIdentity;
import com.ject.user.application.dto.SignupToken;
import com.ject.user.application.exception.InvalidAccessTokenException;
import com.ject.user.application.exception.InvalidSignupTokenException;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSignupTokenProviderTest {

    private static final String SECRET = "test-only-jwt-secret-key-at-least-32-bytes";
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);
    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final SignupIdentity IDENTITY = new SignupIdentity(SocialProvider.NAVER, "social-id-hash");

    @Test
    @DisplayName("발급한 가입 토큰을 검증하면 소셜 종류와 식별값 해시가 나오고, 7일 뒤 만료된다")
    void issueAndVerify() {
        // given
        JwtSignupTokenProvider provider = provider(NOW);

        // when
        SignupToken token = provider.issue(IDENTITY);

        // then
        assertEquals(IDENTITY, provider.verify(token.value()));
        assertEquals(NOW.plus(SEVEN_DAYS), token.expiresAt());
    }

    @Test
    @DisplayName("7일이 지난 가입 토큰은 MEMBER_010")
    void expired() {
        // given
        String token = provider(NOW).issue(IDENTITY).value();

        // when & then
        assertThrows(InvalidSignupTokenException.class,
                () -> provider(NOW.plus(SEVEN_DAYS).plusSeconds(1)).verify(token));
    }

    @Test
    @DisplayName("액세스 토큰이나 위조 토큰은 가입 토큰으로 쓸 수 없다")
    void rejectsOtherTokens() {
        // given
        String accessToken = new JwtAccessTokenProvider(SECRET, Duration.ofHours(1), Clock.fixed(NOW, ZoneOffset.UTC))
                .issue(42L).value();

        // when & then
        assertThrows(InvalidSignupTokenException.class, () -> provider(NOW).verify(accessToken));
        assertThrows(InvalidSignupTokenException.class, () -> provider(NOW).verify("not-a-jwt"));
    }

    @Test
    @DisplayName("가입 토큰은 액세스 토큰으로 쓸 수 없다 (AUTH_003)")
    void signupTokenIsNotAccessToken() {
        // given
        String signupToken = provider(NOW).issue(IDENTITY).value();
        JwtAccessTokenProvider accessTokenProvider = new JwtAccessTokenProvider(SECRET, Duration.ofHours(1),
                Clock.fixed(NOW, ZoneOffset.UTC));

        // when & then
        assertThrows(InvalidAccessTokenException.class, () -> accessTokenProvider.verify(signupToken));
    }

    private static JwtSignupTokenProvider provider(Instant now) {
        return new JwtSignupTokenProvider(SECRET, SEVEN_DAYS, Clock.fixed(now, ZoneOffset.UTC));
    }
}
