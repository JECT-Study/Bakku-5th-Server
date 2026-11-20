package com.ject.user.infrastructure.security;

import com.ject.user.application.port.SocialIdHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * 소셜 회원번호를 HMAC-SHA256(서버 비밀키) 16진수 64자로 바꾼다 (결정 A4-1).
 * 키가 바뀌면 기존 회원을 찾지 못하므로, 한 번 정한 SOCIAL_ID_HASH_SECRET은 바꾸지 않는다.
 */
@Component
public class HmacSocialIdHasher implements SocialIdHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKeySpec key;

    public HmacSocialIdHasher(@Value("${app.auth.social-id-hash-secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("SOCIAL_ID_HASH_SECRET은 32바이트 이상이어야 합니다.");
        }
        this.key = new SecretKeySpec(secretBytes, ALGORITHM);
    }

    @Override
    public String hash(String socialId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(socialId.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("소셜 식별값 해시를 만들지 못했습니다.", e);
        }
    }
}
