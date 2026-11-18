package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.exception.InvalidRefreshTokenException;
import com.ject.user.application.port.AccessTokenProvider;
import com.ject.user.application.port.PushDeviceRepository;
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
 * 로그인 · 가입 · 재발급한 회원에게 액세스 토큰과 이 기기의 리프레시 토큰을 발급한다.
 * 리프레시 토큰은 추측할 수 없는 난수이고, 서버에는 SHA-256 해시만 저장한다 (결정 C2).
 */
@Component
public class LoginTokenIssuer {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final ServiceTime serviceTime;
    private final SecureRandom random = new SecureRandom();

    public LoginTokenIssuer(AccessTokenProvider accessTokenProvider, RefreshTokenRepository refreshTokenRepository,
                            PushDeviceRepository pushDeviceRepository, ServiceTime serviceTime) {
        this.accessTokenProvider = accessTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.pushDeviceRepository = pushDeviceRepository;
        this.serviceTime = serviceTime;
    }

    /**
     * 이 기기에 있던 토큰은 무효가 된다. 다른 회원이 쓰던 기기면 그 회원의 푸시 기기도 지워서
     * 이 기기로 이전 회원 알림이 가지 않게 한다 (결정 A12). 가입처럼 호출하는 쪽에 트랜잭션이 있으면 거기에 합류한다.
     */
    @Transactional
    public LoginTokens issue(Member member, String deviceId) {
        pushDeviceRepository.deleteByDeviceIdOfOtherMembers(deviceId, member.getId());
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
        return toLoginTokens(member, refreshToken);
    }

    /**
     * 저장된 토큰을 새 토큰으로 회전한다 (AU-3). 재발급 서비스의 트랜잭션 안에서 부른다.
     * 여기에 트랜잭션을 따로 걸면 AUTH_005를 던질 때 바깥 트랜잭션까지 롤백 표시가 되어 500이 나므로 걸지 않는다.
     *
     * @throws InvalidRefreshTokenException 다른 요청이 먼저 회전함 (AUTH_005, 결정 C3)
     */
    public LoginTokens rotate(Member member, RefreshToken stored) {
        String refreshToken = newRefreshToken();
        stored.rotate(hash(refreshToken), serviceTime.now());
        if (!refreshTokenRepository.saveRotation(stored)) {
            throw InvalidRefreshTokenException.alreadyReissued();
        }
        return toLoginTokens(member, refreshToken);
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

    private LoginTokens toLoginTokens(Member member, String refreshToken) {
        return new LoginTokens(accessTokenProvider.issue(member.getId()), refreshToken, member.getId(),
                member.getNickname().value());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
