package com.ject.user.application;

import com.ject.user.application.service.AppleTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleTokenServiceTest {

    private FakeAppleTokenClient appleTokenClient;
    private FakeAppleTokenRepository appleTokenRepository;
    private AppleTokenService appleTokenService;

    @BeforeEach
    void setUp() {
        appleTokenClient = new FakeAppleTokenClient();
        appleTokenRepository = new FakeAppleTokenRepository();
        appleTokenService = new AppleTokenService(appleTokenClient, appleTokenRepository);
    }

    @Test
    @DisplayName("authorization code를 Apple 리프레시 토큰으로 바꿔 소셜 계정 기준으로 보관한다")
    void keep() {
        // when
        appleTokenService.keep("hash-1", "code-1");

        // then
        assertEquals(Optional.of("refresh-1"), appleTokenRepository.find("hash-1"));
    }

    @Test
    @DisplayName("다시 로그인하면 새 리프레시 토큰으로 바꾼다")
    void keepReplaces() {
        // when
        appleTokenService.keep("hash-1", "code-1");
        appleTokenService.keep("hash-1", "code-2");

        // then
        assertEquals(Optional.of("refresh-2"), appleTokenRepository.find("hash-1"));
    }

    @Test
    @DisplayName("Apple 토큰 교환이 실패해도 예외를 넘기지 않는다 (로그인은 그대로 진행)")
    void keepFailure() {
        assertDoesNotThrow(() -> appleTokenService.keep("hash-1", "code-fail"));
        assertTrue(appleTokenRepository.find("hash-1").isEmpty());
    }

    @Test
    @DisplayName("탈퇴 때 보관한 토큰을 Apple에서 취소하고 지운다")
    void revoke() {
        // given
        appleTokenService.keep("hash-1", "code-1");

        // when
        appleTokenService.revoke("hash-1");

        // then
        assertEquals(List.of("refresh-1"), appleTokenClient.revoked);
        assertTrue(appleTokenRepository.find("hash-1").isEmpty());
    }

    @Test
    @DisplayName("보관한 토큰이 없으면 Apple을 부르지 않는다")
    void revokeWithoutToken() {
        // when
        appleTokenService.revoke("hash-1");

        // then
        assertTrue(appleTokenClient.revoked.isEmpty());
    }

    @Test
    @DisplayName("Apple 토큰 취소가 실패하면 다시 취소할 수 있게 토큰을 남기고 예외는 넘기지 않는다")
    void revokeFailureKeepsToken() {
        // given
        appleTokenRepository.save("hash-1", "refresh-fail");

        // when & then
        assertDoesNotThrow(() -> appleTokenService.revoke("hash-1"));
        assertEquals(Optional.of("refresh-fail"), appleTokenRepository.find("hash-1"));
    }
}
