package com.ject.user.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HmacSocialIdHasherTest {

    private static final String SECRET = "test-only-social-id-hash-secret-0001";

    @Test
    @DisplayName("같은 회원번호는 항상 같은 16진수 64자 해시가 되고, 원문은 남지 않는다")
    void sameInputSameHash() {
        // given
        HmacSocialIdHasher hasher = new HmacSocialIdHasher(SECRET);

        // when
        String first = hasher.hash("4200");
        String second = hasher.hash("4200");

        // then
        assertEquals(first, second);
        assertEquals(64, first.length());
        assertNotEquals("4200", first);
        assertNotEquals(first, hasher.hash("4201"));
    }

    @Test
    @DisplayName("키가 다르면 해시도 다르다")
    void differentSecretDifferentHash() {
        // given
        HmacSocialIdHasher hasher = new HmacSocialIdHasher(SECRET);
        HmacSocialIdHasher other = new HmacSocialIdHasher("other-social-id-hash-secret-000001");

        // when & then
        assertNotEquals(hasher.hash("4200"), other.hash("4200"));
    }

    @Test
    @DisplayName("키가 32바이트보다 짧으면 만들 수 없다")
    void shortSecret() {
        assertThrows(IllegalStateException.class, () -> new HmacSocialIdHasher("short"));
    }
}
