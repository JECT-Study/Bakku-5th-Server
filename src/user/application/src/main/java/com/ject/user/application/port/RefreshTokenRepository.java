package com.ject.user.application.port;

import com.ject.user.domain.domain.RefreshToken;

import java.util.Optional;

/**
 * 리프레시 토큰 저장소 출력 포트. 기기 ID마다 한 행이다 (결정 C2).
 */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findByDeviceId(String deviceId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByPreviousTokenHash(String previousTokenHash);

    RefreshToken save(RefreshToken refreshToken);

    /**
     * {@link RefreshToken#rotate}한 토큰을 저장한다. 저장된 현재 토큰이 회전 전 토큰(지금의 직전 토큰)일 때만 바꾼다.
     *
     * @return 다른 요청이 먼저 회전해서 바꾸지 못했으면 false
     */
    boolean saveRotation(RefreshToken rotated);

    void delete(RefreshToken refreshToken);
}
