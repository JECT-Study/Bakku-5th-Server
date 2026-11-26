package com.ject.user.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Apple 리프레시 토큰을 AES-256-GCM으로 암호화한다. 결과는 Base64(IV 12바이트 + 암호문)다.
 * 키가 없으면(설정 전) 암호화 · 복호화 때 실패한다. Apple 토큰 교환도 키가 없으면 하지 않는다.
 */
@Component
public class AppleTokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_LENGTH = 32;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    /**
     * @param encodedKey 32바이트 키를 Base64로 인코딩한 값 (환경변수 APPLE_TOKEN_ENCRYPTION_KEY), 비우면 사용하지 않음
     */
    public AppleTokenCipher(@Value("${app.auth.apple.token-encryption-key:}") String encodedKey) {
        this.key = encodedKey.isBlank() ? null : toKey(encodedKey);
    }

    public boolean isEnabled() {
        return key != null;
    }

    public String encrypt(String plainText) {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Apple 토큰을 암호화하지 못했습니다.", e);
        }
    }

    public String decrypt(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, bytes, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(bytes, IV_LENGTH, bytes.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Apple 토큰을 복호화하지 못했습니다.", e);
        }
    }

    private SecretKeySpec requireKey() {
        if (key == null) {
            throw new IllegalStateException("Apple 토큰 암호화 키(APPLE_TOKEN_ENCRYPTION_KEY)가 없습니다.");
        }
        return key;
    }

    private static SecretKeySpec toKey(String encodedKey) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("APPLE_TOKEN_ENCRYPTION_KEY는 32바이트 키를 Base64로 인코딩한 값이어야 합니다.", e);
        }
        if (bytes.length != KEY_LENGTH) {
            throw new IllegalStateException("APPLE_TOKEN_ENCRYPTION_KEY는 32바이트여야 합니다. 지금 " + bytes.length + "바이트");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
