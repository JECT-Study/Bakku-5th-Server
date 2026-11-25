package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.MemberRestriction;
import com.ject.user.application.service.MemberRestrictionService;
import com.ject.user.domain.domain.BirthDate;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberRestrictionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private MemberRestrictionService memberRestrictionService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        memberRestrictionService = new MemberRestrictionService(memberRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("이용 제한 중이면 제한 여부와 해제 일시를 준다")
    void restricted() {
        // given
        Instant until = NOW.plusSeconds(7 * 24 * 3600);
        Long id = save("제한회원", MemberStatus.RESTRICTED, until);

        // when
        MemberRestriction restriction = memberRestrictionService.getRestriction(id);

        // then
        assertEquals(new MemberRestriction(true, null, until), restriction);
    }

    @Test
    @DisplayName("해제 일시가 지난 제한은 제한이 아니다")
    void expired() {
        // given
        Long id = save("풀린회원", MemberStatus.RESTRICTED, NOW.minusSeconds(1));

        // when & then
        assertEquals(MemberRestriction.none(), memberRestrictionService.getRestriction(id));
    }

    @Test
    @DisplayName("이용 중인 회원은 제한이 아니다")
    void active() {
        // given
        Long id = save("바꾸", MemberStatus.ACTIVE, null);

        // when & then
        assertEquals(MemberRestriction.none(), memberRestrictionService.getRestriction(id));
    }

    @Test
    @DisplayName("없는 회원이면 MEMBER_201 예외를 던진다")
    void missingMember() {
        assertThrows(MemberNotFoundException.class, () -> memberRestrictionService.getRestriction(999L));
    }

    private Long save(String nickname, MemberStatus status, Instant restrictedUntil) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, restrictedUntil, 0, false, NOW, null, null,
                NOW, null);
        return memberRepository.save(member).getId();
    }
}
