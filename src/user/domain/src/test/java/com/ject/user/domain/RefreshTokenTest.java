package com.ject.user.domain;

import com.ject.user.domain.domain.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    @Test
    @DisplayName("같은 기기에서 다시 로그인하면 새 회원 · 새 토큰으로 바뀌고 직전 토큰은 남기지 않는다")
    void reissue() {
        // given
        RefreshToken token = RefreshToken.restore(1L, 10L, "device-1", "old-hash", "older-hash",
                NOW.minusSeconds(60));

        // when
        token.reissue(20L, "new-hash", NOW);

        // then
        assertEquals(20L, token.getMemberId());
        assertEquals("device-1", token.getDeviceId());
        assertEquals("new-hash", token.getTokenHash());
        assertNull(token.getPreviousTokenHash());
        assertEquals(NOW, token.getRotatedAt());
    }

    @Test
    @DisplayName("재발급하면 받은 토큰이 직전 토큰이 되고 회전 시각이 바뀐다")
    void rotate() {
        // given
        RefreshToken token = RefreshToken.restore(1L, 10L, "device-1", "old-hash", "older-hash",
                NOW.minusSeconds(60));

        // when
        token.rotate("new-hash", NOW);

        // then
        assertEquals(10L, token.getMemberId());
        assertEquals("new-hash", token.getTokenHash());
        assertEquals("old-hash", token.getPreviousTokenHash());
        assertEquals(NOW, token.getRotatedAt());
    }

    @Test
    @DisplayName("회전 후 10초까지는 동시 재발급, 10초가 지나면 재사용으로 본다")
    void isConcurrentRefresh() {
        // given
        RefreshToken token = RefreshToken.restore(1L, 10L, "device-1", "new-hash", "old-hash", NOW);

        // when & then
        assertTrue(token.isConcurrentRefresh(NOW));
        assertTrue(token.isConcurrentRefresh(NOW.plusSeconds(10)));
        assertFalse(token.isConcurrentRefresh(NOW.plusSeconds(10).plusMillis(1)));
    }
}
