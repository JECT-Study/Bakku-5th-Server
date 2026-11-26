package com.ject.user.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleTokenCipherTest {

    private static final String KEY = Base64.getEncoder()
            .encodeToString("test-only-apple-token-key-000001".getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("암호화한 토큰을 같은 키로 되돌릴 수 있고, 저장되는 값에는 원래 토큰이 보이지 않는다")
    void encryptDecrypt() {
        // given
        AppleTokenCipher cipher = new AppleTokenCipher(KEY);

        // when
        String encrypted = cipher.encrypt("r.apple-refresh-token");

        // then
        assertFalse(encrypted.contains("apple-refresh-token"));
        assertEquals("r.apple-refresh-token", cipher.decrypt(encrypted));
    }

    @Test
    @DisplayName("같은 토큰도 매번 다른 값으로 암호화된다 (IV가 매번 다름)")
    void randomIv() {
        AppleTokenCipher cipher = new AppleTokenCipher(KEY);
        assertNotEquals(cipher.encrypt("token"), cipher.encrypt("token"));
    }

    @Test
    @DisplayName("다른 키로는 복호화할 수 없다")
    void wrongKey() {
        // given
        String encrypted = new AppleTokenCipher(KEY).encrypt("token");
        String otherKey = Base64.getEncoder()
                .encodeToString("test-only-apple-token-key-000002".getBytes(StandardCharsets.UTF_8));

        // when & then
        assertThrows(IllegalStateException.class, () -> new AppleTokenCipher(otherKey).decrypt(encrypted));
    }

    @Test
    @DisplayName("키가 비어 있으면 꺼진 상태이고, 암호화하면 실패한다")
    void disabled() {
        AppleTokenCipher cipher = new AppleTokenCipher("");
        assertFalse(cipher.isEnabled());
        assertThrows(IllegalStateException.class, () -> cipher.encrypt("token"));
        assertTrue(new AppleTokenCipher(KEY).isEnabled());
    }

    @Test
    @DisplayName("32바이트가 아닌 키는 원인을 알려 주며 실패한다")
    void wrongKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new AppleTokenCipher(shortKey));
        assertTrue(e.getMessage().contains("32바이트"));
    }
}
