package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.MarketingConsent;
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.service.MemberService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
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

    private static Member member(String nickname, Gender gender, MemberStatus status) {
        return Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2001, 3, 15)), gender, status, null, 12, true, JOINED_AT, null, null,
                JOINED_AT, null);
    }
}
