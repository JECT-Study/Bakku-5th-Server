package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.MarketingConsent;
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.dto.NicknameChange;
import com.ject.user.application.service.MemberService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.DuplicateNicknameException;
import com.ject.user.domain.exception.InvalidNicknameException;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.domain.exception.NicknameChangeTooSoonException;
import com.ject.user.domain.exception.SameNicknameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T03:00:00Z");
    private static final Instant JOINED_AT = Instant.parse("2026-11-01T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        memberService = new MemberService(memberRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("MB-2 내 정보는 로그인한 회원의 값이다")
    void getMyInfo() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", Gender.FEMALE, MemberStatus.ACTIVE)).getId();
        memberRepository.save(member("하치와레러버", Gender.MALE, MemberStatus.ACTIVE));

        // when
        MyInfo info = memberService.getMyInfo(memberId);

        // then
        assertEquals(memberId, info.userId());
        assertEquals("쿠로미덕후", info.nickname());
        assertEquals("KAKAO", info.provider());
        assertEquals(LocalDate.of(2001, 3, 15), info.birthDate());
        assertEquals("FEMALE", info.gender());
        assertEquals(12, info.completedCount());
        assertEquals(true, info.marketingAgreed());
        assertEquals(JOINED_AT, info.createdAt());
        assertNull(info.nicknameChangeableAt());
    }

    @Test
    @DisplayName("MB-2 성별을 비워 두고 가입했으면 gender는 null")
    void getMyInfo_withoutGender() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", null, MemberStatus.ACTIVE)).getId();

        // when & then
        assertNull(memberService.getMyInfo(memberId).gender());
    }

    @Test
    @DisplayName("MB-2 없는 회원이면 MemberNotFoundException")
    void getMyInfo_unknownMember() {
        assertThrows(MemberNotFoundException.class, () -> memberService.getMyInfo(999L));
    }

    @Test
    @DisplayName("MB-2 탈퇴한 회원이면 MemberNotFoundException")
    void getMyInfo_withdrawnMember() {
        // given
        Member member = memberRepository.save(member("쿠로미덕후", null, MemberStatus.ACTIVE));
        member.withdraw(NOW);
        memberRepository.save(member);

        // when & then
        assertThrows(MemberNotFoundException.class, () -> memberService.getMyInfo(member.getId()));
    }

    @Test
    @DisplayName("MB-7 마케팅 수신 동의를 바꾸면 저장되고 변경 일시는 지금이다")
    void updateMarketingConsent() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", null, MemberStatus.ACTIVE)).getId();

        // when
        MarketingConsent consent = memberService.updateMarketingConsent(memberId, false);

        // then
        assertEquals(new MarketingConsent(false, NOW), consent);
        Member saved = memberRepository.findById(memberId).orElseThrow();
        assertEquals(false, saved.isMarketingAgreed());
        assertEquals(NOW, saved.getMarketingChangedAt());
    }

    @Test
    @DisplayName("MB-7 같은 값을 보내면 변경 일시는 그대로다")
    void updateMarketingConsent_sameValue() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", null, MemberStatus.ACTIVE)).getId();

        // when
        MarketingConsent consent = memberService.updateMarketingConsent(memberId, true);

        // then
        assertEquals(new MarketingConsent(true, JOINED_AT), consent);
    }

    @Test
    @DisplayName("MB-7 탈퇴한 회원이면 MemberNotFoundException")
    void updateMarketingConsent_withdrawnMember() {
        // given
        Member member = memberRepository.save(member("쿠로미덕후", null, MemberStatus.ACTIVE));
        member.withdraw(NOW);
        memberRepository.save(member);

        // when & then
        assertThrows(MemberNotFoundException.class, () -> memberService.updateMarketingConsent(member.getId(), false));
    }

    @Test
    @DisplayName("MB-3 가입 때 정한 닉네임은 바로 바꿀 수 있고, 다음 변경 가능 시각은 30일 뒤다")
    void changeNickname() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", Gender.FEMALE, MemberStatus.ACTIVE)).getId();

        // when
        NicknameChange change = memberService.changeNickname(memberId, "하치와레러버");

        // then
        assertEquals(new NicknameChange("하치와레러버", NOW.plus(Duration.ofDays(30))), change);
        Member saved = memberRepository.findById(memberId).orElseThrow();
        assertEquals("하치와레러버", saved.getNickname().value());
        assertEquals(NOW, saved.getNicknameChangedAt());
    }

    @Test
    @DisplayName("MB-3 마지막 변경 뒤 30일이 지나면 다시 바꿀 수 있다")
    void changeNicknameAfter30Days() {
        // given
        Long memberId = memberRepository.save(changedAt("쿠로미덕후", NOW.minus(Duration.ofDays(30)))).getId();

        // when & then
        assertEquals("하치와레러버", memberService.changeNickname(memberId, "하치와레러버").nickname());
    }

    @Test
    @DisplayName("MB-3 마지막 변경 뒤 30일 안이면 MEMBER_103이고 닉네임은 그대로다")
    void changeNicknameTooSoon() {
        // given
        Long memberId = memberRepository.save(changedAt("쿠로미덕후", NOW.minus(Duration.ofDays(29)))).getId();

        // when & then
        assertThrows(NicknameChangeTooSoonException.class,
                () -> memberService.changeNickname(memberId, "하치와레러버"));
        assertEquals("쿠로미덕후", memberRepository.findById(memberId).orElseThrow().getNickname().value());
    }

    @Test
    @DisplayName("MB-3 확인 뒤 다른 요청이 먼저 바꿨으면(조건부 수정 실패) MEMBER_103이다")
    void changeNicknameRace() {
        // given
        FakeMemberRepository raced = new FakeMemberRepository() {
            @Override
            public boolean changeNickname(Long id, Nickname nickname, Instant changedAt, Instant changedBefore) {
                return false;
            }
        };
        Long memberId = raced.save(member("쿠로미덕후", Gender.FEMALE, MemberStatus.ACTIVE)).getId();
        MemberService service = new MemberService(raced, new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        // when & then
        assertThrows(NicknameChangeTooSoonException.class, () -> service.changeNickname(memberId, "하치와레러버"));
    }

    @Test
    @DisplayName("MB-3 다른 회원이 쓰는 닉네임이면 영문 대소문자가 달라도 MEMBER_102다")
    void changeNicknameDuplicate() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", Gender.FEMALE, MemberStatus.ACTIVE)).getId();
        memberRepository.save(member("Bakku", Gender.MALE, MemberStatus.ACTIVE));

        // when & then
        assertThrows(DuplicateNicknameException.class, () -> memberService.changeNickname(memberId, "bakku"));
    }

    @Test
    @DisplayName("MB-3 내 닉네임의 영문 대소문자만 바꾸는 것은 중복이 아니다")
    void changeNicknameCaseOnly() {
        // given
        Long memberId = memberRepository.save(member("Bakku", Gender.FEMALE, MemberStatus.ACTIVE)).getId();

        // when & then
        assertEquals("bakku", memberService.changeNickname(memberId, "bakku").nickname());
    }

    @Test
    @DisplayName("MB-3 지금 닉네임과 똑같으면 MEMBER_104다")
    void changeNicknameSame() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", Gender.FEMALE, MemberStatus.ACTIVE)).getId();

        // when & then
        assertThrows(SameNicknameException.class, () -> memberService.changeNickname(memberId, "쿠로미덕후"));
    }

    @Test
    @DisplayName("MB-3 형식이 틀리면 MEMBER_101이다")
    void changeNicknameInvalid() {
        // given
        Long memberId = memberRepository.save(member("쿠로미덕후", Gender.FEMALE, MemberStatus.ACTIVE)).getId();

        // when & then
        assertThrows(InvalidNicknameException.class, () -> memberService.changeNickname(memberId, "하 치"));
    }

    private static Member changedAt(String nickname, Instant nicknameChangedAt) {
        return Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2001, 3, 15)), Gender.FEMALE, MemberStatus.ACTIVE, null, 12, true,
                JOINED_AT, nicknameChangedAt, null, JOINED_AT, null);
    }

    private static Member member(String nickname, Gender gender, MemberStatus status) {
        return Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2001, 3, 15)), gender, status, null, 12, true, JOINED_AT, null, null,
                JOINED_AT, null);
    }
}
