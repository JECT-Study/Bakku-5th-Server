package com.ject.user.application;

import com.ject.user.application.dto.NicknameAvailability;
import com.ject.user.application.dto.NicknameAvailability.UnavailableReason;
import com.ject.user.application.service.NicknameService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NicknameServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private NicknameService nicknameService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        nicknameService = new NicknameService(memberRepository);
    }

    @Test
    @DisplayName("규칙에 맞고 아무도 쓰지 않는 닉네임이면 사용할 수 있다")
    void available() {
        // when
        NicknameAvailability result = nicknameService.checkAvailability("하치와레러버");

        // then
        assertEquals(NicknameAvailability.usable(), result);
    }

    @Test
    @DisplayName("글자 수 · 형식 오류는 이유를 나눠서 준다")
    void ruleViolation() {
        // when & then
        assertEquals(NicknameAvailability.unusable(UnavailableReason.INVALID_LENGTH),
                nicknameService.checkAvailability("가"));
        assertEquals(NicknameAvailability.unusable(UnavailableReason.INVALID_FORMAT),
                nicknameService.checkAvailability("닉 네임"));
    }

    @Test
    @DisplayName("다른 회원이 쓰는 닉네임이면 영문 대소문자가 달라도 중복이다")
    void duplicateIgnoresCase() {
        // given
        save("Bakku");

        // when
        NicknameAvailability result = nicknameService.checkAvailability("bAKKU");

        // then
        assertEquals(NicknameAvailability.unusable(UnavailableReason.DUPLICATE), result);
    }

    @Test
    @DisplayName("탈퇴한 회원이 쓰던 닉네임은 바로 다시 쓸 수 있다")
    void withdrawnNicknameReusable() {
        // given
        Member member = save("떠난회원");
        member.withdraw(NOW);
        memberRepository.save(member);

        // when
        NicknameAvailability result = nicknameService.checkAvailability("떠난회원");

        // then
        assertEquals(NicknameAvailability.usable(), result);
    }

    private Member save(String nickname) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, 0, false, NOW, null, null,
                NOW, null);
        return memberRepository.save(member);
    }
}
