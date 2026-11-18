package com.ject.user.application.service;

import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.application.port.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃 (AU-4). 리프레시 토큰과 이 기기의 푸시 기기를 지워 로그아웃한 기기로는 푸시를 보내지 않는다 (G-09-1).
 */
@Service
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PushDeviceRepository pushDeviceRepository;

    public LogoutService(RefreshTokenRepository refreshTokenRepository, PushDeviceRepository pushDeviceRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.pushDeviceRepository = pushDeviceRepository;
    }

    /**
     * 로그인한 회원의 것만 지운다. FE는 서버 응답과 관계없이 로그아웃하므로 이미 지워졌거나 없어도 에러 없이 끝낸다.
     * 재발급 응답을 받지 못한 기기도 로그아웃할 수 있게 직전 토큰으로도 찾는다.
     */
    @Transactional
    public void logout(Long memberId, String refreshToken, String deviceId) {
        String tokenHash = LoginTokenIssuer.hash(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .or(() -> refreshTokenRepository.findByPreviousTokenHash(tokenHash))
                .filter(token -> token.getMemberId().equals(memberId))
                .ifPresent(refreshTokenRepository::delete);
        pushDeviceRepository.deleteByDeviceIdAndMemberId(deviceId, memberId);
    }
}
