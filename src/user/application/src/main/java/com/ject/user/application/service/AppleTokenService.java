package com.ject.user.application.service;

import com.ject.user.application.port.AppleTokenClient;
import com.ject.user.application.port.AppleTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sign in with Apple 앱은 계정을 삭제할 때 Apple 토큰을 취소해야 한다 (애플 심사 지침 5.1.1(v)).
 * 로그인 때 authorization code를 리프레시 토큰으로 바꿔 두고, 탈퇴 때 그 토큰을 취소한다 (AU-1 · G-09-2).
 * 로그인 · 탈퇴 자체를 막지 않도록 실패는 로그로만 남긴다.
 */
@Service
public class AppleTokenService {

    private static final Logger log = LoggerFactory.getLogger(AppleTokenService.class);

    private final AppleTokenClient appleTokenClient;
    private final AppleTokenRepository appleTokenRepository;

    public AppleTokenService(AppleTokenClient appleTokenClient, AppleTokenRepository appleTokenRepository) {
        this.appleTokenClient = appleTokenClient;
        this.appleTokenRepository = appleTokenRepository;
    }

    /**
     * 실패하면 이 계정은 탈퇴 때 Apple 토큰을 취소할 수 없어 WARN으로 남긴다. 다음 로그인 때 다시 받는다.
     */
    public void keep(String socialIdHash, String authorizationCode) {
        try {
            appleTokenClient.exchange(authorizationCode)
                    .ifPresent(refreshToken -> appleTokenRepository.save(socialIdHash, refreshToken));
        } catch (RuntimeException e) {
            log.warn("Apple 리프레시 토큰을 받지 못했습니다. 탈퇴 때 Apple 토큰을 취소할 수 없습니다.", e);
        }
    }

    /**
     * 취소에 성공하면 보관한 토큰을 지운다. 실패하면 다시 취소할 수 있게 토큰을 남기고 ERROR로 남긴다.
     */
    public void revoke(String socialIdHash) {
        appleTokenRepository.find(socialIdHash).ifPresent(refreshToken -> {
            try {
                appleTokenClient.revoke(refreshToken);
                appleTokenRepository.delete(socialIdHash);
            } catch (RuntimeException e) {
                log.error("Apple 토큰 취소 실패. 보관한 토큰으로 다시 취소해야 합니다.", e);
            }
        });
    }
}
