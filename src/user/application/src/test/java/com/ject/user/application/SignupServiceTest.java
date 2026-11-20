package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.dto.SignupCommand;
import com.ject.user.application.dto.SignupCommand.Agreement;
import com.ject.user.application.exception.InvalidSignupTokenException;
import com.ject.user.application.exception.RejoinNotAvailableException;
import com.ject.user.application.service.LoginTokenIssuer;
import com.ject.user.application.service.SignupService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.AlreadySignedUpException;
import com.ject.user.domain.exception.DuplicateNicknameException;
import com.ject.user.domain.exception.InvalidNicknameException;
import com.ject.user.domain.exception.TermsAgreementException;
import com.ject.user.domain.exception.UnderAgeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignupServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final String SIGNUP_TOKEN = "signup:KAKAO:hash-42";
    private static final List<Agreement> AGREEMENTS = List.of(
            new Agreement("SERVICE", "1.0", true),
            new Agreement("PRIVACY_COLLECTION", "1.0", true),
            new Agreement("AGE_14", "1.0", true),
            new Agreement("MARKETING", "1.0", true));

    private FakeMemberRepository memberRepository;
    private FakeTermsAgreementRepository termsAgreementRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;
    private SignupService signupService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        termsAgreementRepository = new FakeTermsAgreementRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        LoginTokenIssuer issuer = new LoginTokenIssuer(new FakeAccessTokenProvider(), refreshTokenRepository,
                serviceTime);
        signupService = new SignupService(new FakeSignupTokenProvider(), memberRepository, termsAgreementRepository,
                issuer, serviceTime);
    }

    @Test
    @DisplayName("가입하면 회원 · 약관 동의 · 이 기기의 리프레시 토큰을 저장하고 토큰을 준다")
    void signup() {
        // when
        LoginTokens tokens = signupService.signup(command(SIGNUP_TOKEN, AGREEMENTS, "쿠로미덕후", "FEMALE"));

        // then
        Member member = memberRepository.findById(tokens.memberId()).orElseThrow();
        assertEquals(SocialProvider.KAKAO, member.getProvider());
        assertEquals("hash-42", member.getSocialIdHash());
        assertEquals("쿠로미덕후", tokens.nickname());
        assertEquals(Gender.FEMALE, member.getGender());
        assertTrue(member.isMarketingAgreed());
        assertNull(member.getPreviousMemberId());
        assertEquals(4, termsAgreementRepository.findByMemberId(member.getId()).size());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @DisplayName("성별은 비워 둘 수 있고, 마케팅에 동의하지 않으면 수신 동의도 false다")
    void signupWithoutGenderAndMarketing() {
        // given
        List<Agreement> agreements = List.of(
                new Agreement("SERVICE", "1.0", true),
                new Agreement("PRIVACY_COLLECTION", "1.0", true),
                new Agreement("AGE_14", "1.0", true),
                new Agreement("MARKETING", "1.0", false));

        // when
        LoginTokens tokens = signupService.signup(command(SIGNUP_TOKEN, agreements, "쿠로미덕후", null));

        // then
        Member member = memberRepository.findById(tokens.memberId()).orElseThrow();
        assertNull(member.getGender());
        assertFalse(member.isMarketingAgreed());
    }

    @Test
    @DisplayName("탈퇴 후 7일이 지나 다시 가입하면 이전 회원 id를 연결한다")
    void rejoinLinksPreviousMember() {
        // given
        Member withdrawn = saveMember("hash-42", "떠난회원");
        withdrawn.withdraw(NOW.minus(Duration.ofDays(8)));
        memberRepository.save(withdrawn);

        // when
        LoginTokens tokens = signupService.signup(command(SIGNUP_TOKEN, AGREEMENTS, "돌아온회원", null));

        // then
        assertEquals(withdrawn.getId(), memberRepository.findById(tokens.memberId()).orElseThrow()
                .getPreviousMemberId());
    }

    @Test
    @DisplayName("가입 토큰을 받은 뒤 탈퇴해서 7일이 안 지났으면 MEMBER_002")
    void rejoinWithin7Days() {
        // given
        Member withdrawn = saveMember("hash-42", "떠난회원");
        withdrawn.withdraw(NOW.minus(Duration.ofDays(1)));
        memberRepository.save(withdrawn);

        // when & then
        assertThrows(RejoinNotAvailableException.class,
                () -> signupService.signup(command(SIGNUP_TOKEN, AGREEMENTS, "돌아온회원", null)));
    }

    @Test
    @DisplayName("가입 토큰이 올바르지 않으면 MEMBER_010")
    void invalidSignupToken() {
        assertThrows(InvalidSignupTokenException.class,
                () -> signupService.signup(command("forged", AGREEMENTS, "쿠로미덕후", null)));
    }

    @Test
    @DisplayName("이미 가입된 소셜 계정이면 MEMBER_014")
    void alreadySignedUp() {
        // given
        saveMember("hash-42", "먼저가입");

        // when & then
        assertThrows(AlreadySignedUpException.class,
                () -> signupService.signup(command(SIGNUP_TOKEN, AGREEMENTS, "쿠로미덕후", null)));
    }

    @Test
    @DisplayName("필수 약관이 빠지거나 동의하지 않으면 MEMBER_011, 버전이 최신이 아니면 MEMBER_012")
    void invalidAgreements() {
        // given
        List<Agreement> missingAge = List.of(
                new Agreement("SERVICE", "1.0", true),
                new Agreement("PRIVACY_COLLECTION", "1.0", true));
        List<Agreement> notAgreed = List.of(
                new Agreement("SERVICE", "1.0", false),
                new Agreement("PRIVACY_COLLECTION", "1.0", true),
                new Agreement("AGE_14", "1.0", true));
        List<Agreement> oldVersion = List.of(
                new Agreement("SERVICE", "0.9", true),
                new Agreement("PRIVACY_COLLECTION", "1.0", true),
                new Agreement("AGE_14", "1.0", true));

        // when & then
        assertEquals("MEMBER_011", signupFailure(missingAge).getErrorCode().getCode());
        assertEquals("MEMBER_011", signupFailure(notAgreed).getErrorCode().getCode());
        assertEquals("MEMBER_012", signupFailure(oldVersion).getErrorCode().getCode());
    }

    @Test
    @DisplayName("닉네임 형식이 틀리면 MEMBER_101, 다른 회원이 쓰고 있으면 MEMBER_102")
    void invalidNickname() {
        // given
        saveMember("hash-other", "쿠로미덕후");

        // when & then
        assertThrows(InvalidNicknameException.class,
                () -> signupService.signup(command(SIGNUP_TOKEN, AGREEMENTS, "닉 네임", null)));
        assertThrows(DuplicateNicknameException.class,
                () -> signupService.signup(command(SIGNUP_TOKEN, AGREEMENTS, "쿠로미덕후", null)));
    }

    @Test
    @DisplayName("한국 시간 기준 만 14세 미만이면 MEMBER_013")
    void underAge() {
        // given
        SignupCommand command = new SignupCommand(SIGNUP_TOKEN, AGREEMENTS, "쿠로미덕후", LocalDate.of(2012, 11, 16),
                null, "device-1");

        // when & then
        assertThrows(UnderAgeException.class, () -> signupService.signup(command));
    }

    private TermsAgreementException signupFailure(List<Agreement> agreements) {
        return assertThrows(TermsAgreementException.class,
                () -> signupService.signup(command(SIGNUP_TOKEN, agreements, "쿠로미덕후", null)));
    }

    private static SignupCommand command(String token, List<Agreement> agreements, String nickname, String gender) {
        return new SignupCommand(token, agreements, nickname, LocalDate.of(2001, 3, 15), gender, "device-1");
    }

    private Member saveMember(String socialIdHash, String nickname) {
        return memberRepository.save(Member.restore(null, SocialProvider.KAKAO, socialIdHash, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, 0, false, NOW, null, null,
                NOW, null));
    }
}
