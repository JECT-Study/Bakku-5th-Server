package com.ject.user.infrastructure.security;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.user.application.dto.AccessToken;
import com.ject.user.application.exception.InvalidAccessTokenException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtAccessTokenProviderTest {

    private static final String SECRET = "test-only-jwt-secret-key-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00.700Z");
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    @Test
    @DisplayName("발급한 토큰을 검증하면 회원 id가 나오고, 만료 시각은 발급 1시간 뒤(초 단위)다")
    void issueAndVerify() {
        // given
        JwtAccessTokenProvider provider = providerAt(NOW);

        // when
        AccessToken token = provider.issue(42L);

        // then
        assertEquals(42L, provider.verify(token.value()));
        assertEquals(Instant.parse("2026-11-12T04:00:00Z"), token.expiresAt());
    }

    @Test
    @DisplayName("만료된 토큰은 AUTH_002")
    void expired() {
        // given
        AccessToken token = providerAt(NOW).issue(42L);
        JwtAccessTokenProvider later = providerAt(NOW.plus(ONE_HOUR).plusSeconds(1));

        // when
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class,
                () -> later.verify(token.value()));

        // then
        assertEquals(CommonErrorCode.TOKEN_EXPIRED, e.getErrorCode());
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰 · 형식이 깨진 토큰은 AUTH_003")
    void invalid() {
        // given
        JwtAccessTokenProvider provider = providerAt(NOW);
        String otherKeyToken = new JwtAccessTokenProvider("another-test-only-secret-key-32-bytes!!", ONE_HOUR,
                Clock.fixed(NOW, ZoneOffset.UTC)).issue(42L).value();

        // when & then
        assertEquals(CommonErrorCode.TOKEN_INVALID,
                assertThrows(InvalidAccessTokenException.class, () -> provider.verify(otherKeyToken)).getErrorCode());
        assertEquals(CommonErrorCode.TOKEN_INVALID,
                assertThrows(InvalidAccessTokenException.class, () -> provider.verify("not-a-jwt")).getErrorCode());
    }

    @Test
    @DisplayName("서명 키가 32바이트보다 짧으면 앱이 뜨지 않는다")
    void weakSecret() {
        assertThrows(WeakKeyException.class,
                () -> new JwtAccessTokenProvider("short-secret", ONE_HOUR, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private static JwtAccessTokenProvider providerAt(Instant now) {
        return new JwtAccessTokenProvider(SECRET, ONE_HOUR, Clock.fixed(now, ZoneOffset.UTC));
    }
}
