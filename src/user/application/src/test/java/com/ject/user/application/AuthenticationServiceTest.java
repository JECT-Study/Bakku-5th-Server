package com.ject.user.application;

import com.ject.user.application.exception.InvalidAccessTokenException;
import com.ject.user.application.service.AuthenticationService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberBannedException;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        authenticationService = new AuthenticationService(new FakeAccessTokenProvider(), memberRepository);
    }

    @Test
    @DisplayName("이용 중인 회원 · 이용 제한 회원은 회원 id를 돌려준다")
    void activeOrRestricted() {
        // given
        Long activeId = save("이용중", MemberStatus.ACTIVE);
        Long restrictedId = save("제한중", MemberStatus.RESTRICTED);

        // when & then
        assertEquals(activeId, authenticationService.authenticate("token-" + activeId));
        assertEquals(restrictedId, authenticationService.authenticate("token-" + restrictedId));
    }

    @Test
    @DisplayName("영구 정지 회원은 MEMBER_003")
    void banned() {
        // given
        Long id = save("정지회원", MemberStatus.BANNED);

        // when & then
        assertThrows(MemberBannedException.class, () -> authenticationService.authenticate("token-" + id));
    }

    @Test
    @DisplayName("탈퇴한 회원 · 없는 회원은 MEMBER_201")
    void withdrawnOrMissing() {
        // given
        Long id = save("떠난회원", MemberStatus.ACTIVE);
        Member member = memberRepository.findById(id).orElseThrow();
        member.withdraw(NOW);
        memberRepository.save(member);

        // when & then
        assertThrows(MemberNotFoundException.class, () -> authenticationService.authenticate("token-" + id));
        assertThrows(MemberNotFoundException.class, () -> authenticationService.authenticate("token-999"));
    }

    @Test
    @DisplayName("토큰 검증에 실패하면 회원을 조회하지 않고 그대로 실패한다")
    void invalidToken() {
        assertThrows(InvalidAccessTokenException.class, () -> authenticationService.authenticate("broken"));
    }

    private Long save(String nickname, MemberStatus status) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, 0, false, NOW, null, null, NOW, null);
        return memberRepository.save(member).getId();
    }
}
