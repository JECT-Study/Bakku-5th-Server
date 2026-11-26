package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.SocialLoginResult;
import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.application.exception.RejoinNotAvailableException;
import com.ject.user.application.service.AppleTokenService;
import com.ject.user.application.service.LoginTokenIssuer;
import com.ject.user.application.service.SocialLoginService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberBannedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialLoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final String DEVICE_ID = "device-1";

    private FakeMemberRepository memberRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;
    private FakeAppleTokenClient appleTokenClient;
    private FakeAppleTokenRepository appleTokenRepository;
    private SocialLoginService socialLoginService;
    private int memberSequence;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
        appleTokenClient = new FakeAppleTokenClient();
        appleTokenRepository = new FakeAppleTokenRepository();
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        LoginTokenIssuer issuer = new LoginTokenIssuer(new FakeAccessTokenProvider(), refreshTokenRepository,
                new FakePushDeviceRepository(), serviceTime);
        // 소셜 토큰 "{provider}-{회원번호}"를 그 회원번호로 보고, 네이버만 생년월일 · 성별을 준다. 해시는 "hash-{회원번호}"
        socialLoginService = new SocialLoginService(
                (provider, token) -> {
                    if (!token.startsWith(provider.name().toLowerCase() + "-")) {
                        throw new InvalidSocialTokenException("다른 소셜 토큰");
                    }
                    String socialId = token.substring(token.indexOf('-') + 1);
                    return provider == SocialProvider.NAVER
                            ? new SocialUser(provider, socialId, LocalDate.of(2001, 3, 15), Gender.FEMALE)
                            : new SocialUser(provider, socialId, null, null);
                },
                socialId -> "hash-" + socialId,
                memberRepository, new FakeSignupTokenProvider(), issuer,
                new AppleTokenService(appleTokenClient, appleTokenRepository), serviceTime);
    }

    @Test
    @DisplayName("가입한 소셜 계정이면 액세스 토큰과 이 기기의 리프레시 토큰을 발급한다")
    void login_existingMember() {
        // given
        Member member = save(SocialProvider.KAKAO, "hash-42", MemberStatus.ACTIVE);

        // when
        SocialLoginResult result = socialLoginService.login("kakao", "kakao-42", DEVICE_ID);

        // then
        assertFalse(result.signupRequired());
        assertEquals(member.getId(), result.tokens().memberId());
        assertEquals("token-" + member.getId(), result.tokens().accessToken().value());
        assertEquals(1, refreshTokenRepository.count());
        assertEquals(member.getId(), refreshTokenRepository.findByDeviceId(DEVICE_ID).orElseThrow().getMemberId());
    }

    @Test
    @DisplayName("처음 보는 소셜 계정이면 가입 토큰을 주고, 네이버는 생년월일 · 성별을 미리 채운다")
    void login_newNaverAccount() {
        // when
        SocialLoginResult result = socialLoginService.login("naver", "naver-7", DEVICE_ID);

        // then
        assertTrue(result.signupRequired());
        assertEquals("signup:NAVER:hash-7", result.signupToken().value());
        assertEquals(LocalDate.of(2001, 3, 15), result.prefillBirthDate());
        assertEquals("FEMALE", result.prefillGender());
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    @DisplayName("같은 회원번호라도 다른 소셜 계정이면 별개라서 가입이 필요하다")
    void login_otherProviderIsAnotherAccount() {
        // given
        save(SocialProvider.KAKAO, "hash-42", MemberStatus.ACTIVE);

        // when
        SocialLoginResult result = socialLoginService.login("naver", "naver-42", DEVICE_ID);

        // then
        assertTrue(result.signupRequired());
    }

    @Test
    @DisplayName("영구 정지 회원이면 MEMBER_003")
    void login_banned() {
        // given
        save(SocialProvider.KAKAO, "hash-42", MemberStatus.BANNED);

        // when & then
        assertThrows(MemberBannedException.class, () -> socialLoginService.login("kakao", "kakao-42", DEVICE_ID));
    }

    @Test
    @DisplayName("탈퇴 후 7일이 지나지 않았으면 MEMBER_002와 다시 가입할 수 있는 시각을 준다")
    void login_withdrawnWithin7Days() {
        // given
        withdraw(save(SocialProvider.KAKAO, "hash-42", MemberStatus.ACTIVE), NOW.minus(Duration.ofDays(6)));

        // when
        RejoinNotAvailableException e = assertThrows(RejoinNotAvailableException.class,
                () -> socialLoginService.login("kakao", "kakao-42", DEVICE_ID));

        // then
        assertEquals(OffsetDateTime.parse("2026-11-16T12:00:00+09:00"), e.getRejoinAvailableAt());
    }

    @Test
    @DisplayName("탈퇴 후 7일이 지났으면 새로 가입하도록 가입 토큰을 준다")
    void login_withdrawnAfter7Days() {
        // given
        withdraw(save(SocialProvider.KAKAO, "hash-42", MemberStatus.ACTIVE), NOW.minus(Duration.ofDays(7)));

        // when
        SocialLoginResult result = socialLoginService.login("kakao", "kakao-42", DEVICE_ID);

        // then
        assertTrue(result.signupRequired());
        assertNull(result.prefillBirthDate());
    }

    @Test
    @DisplayName("같은 기기에서 다른 회원이 로그인하면 그 기기의 리프레시 토큰은 새 회원 것이 된다")
    void login_sameDeviceOtherMember() {
        // given
        save(SocialProvider.KAKAO, "hash-1", MemberStatus.ACTIVE);
        Member second = save(SocialProvider.KAKAO, "hash-2", MemberStatus.ACTIVE);
        socialLoginService.login("kakao", "kakao-1", DEVICE_ID);

        // when
        socialLoginService.login("kakao", "kakao-2", DEVICE_ID);

        // then
        assertEquals(1, refreshTokenRepository.count());
        assertEquals(second.getId(), refreshTokenRepository.findByDeviceId(DEVICE_ID).orElseThrow().getMemberId());
    }

    @Test
    @DisplayName("Apple로 처음 로그인하면 가입 토큰을 주고, 가입 전이어도 Apple 리프레시 토큰을 받아 둔다")
    void loginWithApple_signupRequired() {
        // when
        SocialLoginResult result = socialLoginService.loginWithApple("apple-77", "code-77", DEVICE_ID);

        // then
        assertTrue(result.signupRequired());
        assertEquals(Optional.of("refresh-77"), appleTokenRepository.find("hash-77"));
    }

    @Test
    @DisplayName("Apple로 가입한 회원이 로그인하면 토큰을 발급하고 Apple 리프레시 토큰을 새로 바꿔 둔다")
    void loginWithApple_existingMember() {
        // given
        save(SocialProvider.APPLE, "hash-77", MemberStatus.ACTIVE);
        appleTokenRepository.save("hash-77", "refresh-old");

        // when
        SocialLoginResult result = socialLoginService.loginWithApple("apple-77", "code-77", DEVICE_ID);

        // then
        assertFalse(result.signupRequired());
        assertEquals(Optional.of("refresh-77"), appleTokenRepository.find("hash-77"));
    }

    @Test
    @DisplayName("Apple 토큰 교환이 실패해도 로그인은 된다")
    void loginWithApple_exchangeFailure() {
        // given
        save(SocialProvider.APPLE, "hash-77", MemberStatus.ACTIVE);

        // when
        SocialLoginResult result = socialLoginService.loginWithApple("apple-77", "code-fail", DEVICE_ID);

        // then
        assertFalse(result.signupRequired());
        assertTrue(appleTokenRepository.find("hash-77").isEmpty());
    }

    @Test
    @DisplayName("정지된 Apple 회원이면 MEMBER_003이고 Apple 토큰도 받지 않는다")
    void loginWithApple_banned() {
        // given
        save(SocialProvider.APPLE, "hash-77", MemberStatus.BANNED);

        // when & then
        assertThrows(MemberBannedException.class,
                () -> socialLoginService.loginWithApple("apple-77", "code-77", DEVICE_ID));
        assertTrue(appleTokenRepository.find("hash-77").isEmpty());
    }

    private Member save(SocialProvider provider, String socialIdHash, MemberStatus status) {
        Member member = Member.restore(null, provider, socialIdHash, new Nickname("회원" + (++memberSequence)),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, 0, false, NOW, null, null, NOW, null);
        return memberRepository.save(member);
    }

    private void withdraw(Member member, Instant withdrawnAt) {
        member.withdraw(withdrawnAt);
        memberRepository.save(member);
    }
}
