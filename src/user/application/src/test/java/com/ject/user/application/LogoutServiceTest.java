package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.service.LoginTokenIssuer;
import com.ject.user.application.service.LogoutService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.DevicePlatform;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.PushDevice;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T03:00:00Z");
    private static final String DEVICE_ID = "device-1";

    private FakeMemberRepository memberRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;
    private FakePushDeviceRepository pushDeviceRepository;
    private LoginTokenIssuer issuer;
    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
        pushDeviceRepository = new FakePushDeviceRepository();
        issuer = new LoginTokenIssuer(new FakeAccessTokenProvider(), refreshTokenRepository, pushDeviceRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
        logoutService = new LogoutService(refreshTokenRepository, pushDeviceRepository);
    }

    @Test
    @DisplayName("리프레시 토큰과 이 기기의 푸시 기기를 지운다")
    void logout() {
        // given
        Member member = save("hash-1");
        String refreshToken = issuer.issue(member, DEVICE_ID).refreshToken();
        pushDeviceRepository.save(device(member, DEVICE_ID, "fcm-1"));

        // when
        logoutService.logout(member.getId(), refreshToken, DEVICE_ID);

        // then
        assertEquals(0, refreshTokenRepository.count());
        assertEquals(0, pushDeviceRepository.count());
    }

    @Test
    @DisplayName("다른 회원의 리프레시 토큰 · 푸시 기기는 지우지 않는다")
    void logout_otherMembersToken_notDeleted() {
        // given
        Member owner = save("hash-1");
        Member other = save("hash-2");
        String refreshToken = issuer.issue(owner, DEVICE_ID).refreshToken();
        pushDeviceRepository.save(device(owner, DEVICE_ID, "fcm-1"));

        // when
        logoutService.logout(other.getId(), refreshToken, DEVICE_ID);

        // then
        assertEquals(1, refreshTokenRepository.count());
        assertTrue(pushDeviceRepository.findByDeviceId(DEVICE_ID).isPresent());
    }

    @Test
    @DisplayName("이미 로그아웃했어도 에러 없이 끝난다")
    void logout_alreadyLoggedOut() {
        // given
        Member member = save("hash-1");

        // when & then
        assertDoesNotThrow(() -> logoutService.logout(member.getId(), "unknown-token", DEVICE_ID));
    }

    private static PushDevice device(Member member, String deviceId, String pushToken) {
        return PushDevice.register(member.getId(), deviceId, pushToken, DevicePlatform.IOS, "1.0.0");
    }

    private Member save(String socialIdHash) {
        Nickname nickname = new Nickname("회원" + socialIdHash.substring("hash-".length()));
        Member member = Member.restore(null, SocialProvider.KAKAO, socialIdHash, nickname,
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, 0, false, NOW, null, null,
                NOW, null);
        return memberRepository.save(member);
    }
}
