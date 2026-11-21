package com.ject.user.application.port;

import com.ject.user.domain.domain.RefreshToken;

import java.util.Optional;

/**
 * 리프레시 토큰 저장소 출력 포트. 기기 ID마다 한 행이다 (결정 C2).
 */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findByDeviceId(String deviceId);

    RefreshToken save(RefreshToken refreshToken);
}
