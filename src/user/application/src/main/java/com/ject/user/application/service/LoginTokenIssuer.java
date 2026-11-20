package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.port.AccessTokenProvider;
import com.ject.user.application.port.RefreshTokenRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.RefreshToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 로그인 · 가입한 회원에게 액세스 토큰과 이 기기의 리프레시 토큰을 발급한다.
 * 리프레시 토큰은 추측할 수 없는 난수이고, 서버에는 SHA-256 해시만 저장한다 (결정 C2).
 */
@Component
public class LoginTokenIssuer {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ServiceTime serviceTime;
    private final SecureRandom random = new SecureRandom();

    public LoginTokenIssuer(AccessTokenProvider accessTokenProvider, RefreshTokenRepository refreshTokenRepository,
                            ServiceTime serviceTime) {
        this.accessTokenProvider = accessTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.serviceTime = serviceTime;
    }

    /**
     * 이 기기에 있던 토큰은 무효가 된다. 가입처럼 호출하는 쪽에 트랜잭션이 있으면 거기에 합류한다.
     */
    @Transactional
    public LoginTokens issue(Member member, String deviceId) {
        String refreshToken = newRefreshToken();
        String tokenHash = hash(refreshToken);
        Instant now = serviceTime.now();
        RefreshToken stored = refreshTokenRepository.findByDeviceId(deviceId)
                .map(existing -> {
                    existing.reissue(member.getId(), tokenHash, now);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.issue(member.getId(), deviceId, tokenHash, now));
        refreshTokenRepository.save(stored);
        return new LoginTokens(accessTokenProvider.issue(member.getId()), refreshToken, member.getId(),
                member.getNickname().value());
    }

    /**
     * 재발급 · 로그아웃 때 받은 토큰을 저장된 해시와 비교하는 데 쓴다.
     */
    public static String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다.", e);
        }
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
