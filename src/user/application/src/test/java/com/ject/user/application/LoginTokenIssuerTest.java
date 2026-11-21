package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.service.LoginTokenIssuer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T03:00:00Z");
    private static final String DEVICE_ID = "device-1";

    private FakeMemberRepository memberRepository;
    private FakePushDeviceRepository pushDeviceRepository;
    private LoginTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        pushDeviceRepository = new FakePushDeviceRepository();
        issuer = new LoginTokenIssuer(new FakeAccessTokenProvider(), new FakeRefreshTokenRepository(),
                pushDeviceRepository, new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("다른 회원이 쓰던 기기로 로그인하면 이전 회원의 푸시 기기를 지운다")
    void issue_deviceOfOtherMember_deletesTheirPushDevice() {
        // given
        Member previous = save("hash-1");
        Member next = save("hash-2");
        pushDeviceRepository.save(PushDevice.register(previous.getId(), DEVICE_ID, "fcm-1", DevicePlatform.IOS, null));

        // when
        issuer.issue(next, DEVICE_ID);

        // then
        assertTrue(pushDeviceRepository.findByDeviceId(DEVICE_ID).isEmpty());
    }

    @Test
    @DisplayName("같은 회원이 다시 로그인하면 푸시 기기를 그대로 둔다")
    void issue_sameMember_keepsPushDevice() {
        // given
        Member member = save("hash-1");
        pushDeviceRepository.save(PushDevice.register(member.getId(), DEVICE_ID, "fcm-1", DevicePlatform.IOS, null));

        // when
        issuer.issue(member, DEVICE_ID);

        // then
        assertEquals(member.getId(), pushDeviceRepository.findByDeviceId(DEVICE_ID).orElseThrow().getMemberId());
    }

    private Member save(String socialIdHash) {
        Nickname nickname = new Nickname("회원" + socialIdHash.substring("hash-".length()));
        Member member = Member.restore(null, SocialProvider.KAKAO, socialIdHash, nickname,
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, 0, false, NOW, null, null,
                NOW, null);
        return memberRepository.save(member);
    }
}
