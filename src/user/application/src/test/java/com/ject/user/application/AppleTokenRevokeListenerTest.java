package com.ject.user.application;

import com.ject.common.event.member.MemberWithdrawn;
import com.ject.user.application.event.AppleTokenRevokeListener;
import com.ject.user.application.service.AppleTokenService;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppleTokenRevokeListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private FakeAppleTokenClient appleTokenClient;
    private FakeAppleTokenRepository appleTokenRepository;
    private AppleTokenRevokeListener listener;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        appleTokenClient = new FakeAppleTokenClient();
        appleTokenRepository = new FakeAppleTokenRepository();
        listener = new AppleTokenRevokeListener(memberRepository,
                new AppleTokenService(appleTokenClient, appleTokenRepository));
    }

    @Test
    @DisplayName("Apple로 가입한 회원이 탈퇴하면 탈퇴한 회원 행의 소셜 식별값으로 Apple 토큰을 취소한다")
    void appleMemberWithdrawn() {
        // given
        Long memberId = withdrawnMember(SocialProvider.APPLE, "hash-apple");
        appleTokenRepository.save("hash-apple", "refresh-apple");

        // when
        listener.handle(new MemberWithdrawn(memberId, NOW));

        // then
        assertEquals(List.of("refresh-apple"), appleTokenClient.revoked);
        assertTrue(appleTokenRepository.find("hash-apple").isEmpty());
    }

    @Test
    @DisplayName("카카오 · 네이버 회원은 Apple을 부르지 않는다")
    void otherProviderWithdrawn() {
        // given
        Long memberId = withdrawnMember(SocialProvider.KAKAO, "hash-kakao");

        // when
        listener.handle(new MemberWithdrawn(memberId, NOW));

        // then
        assertTrue(appleTokenClient.revoked.isEmpty());
    }

    @Test
    @DisplayName("처리 중 실패해도 예외를 넘기지 않는다 (탈퇴는 이미 커밋됨)")
    void failureIsNotPropagated() {
        // given
        AppleTokenRevokeListener failing = new AppleTokenRevokeListener(new FakeMemberRepository() {
            @Override
            public Optional<Member> findById(Long id) {
                throw new IllegalStateException("DB 오류");
            }
        }, new AppleTokenService(appleTokenClient, appleTokenRepository));

        // when & then
        assertDoesNotThrow(() -> failing.handle(new MemberWithdrawn(1L, NOW)));
    }

    private Long withdrawnMember(SocialProvider provider, String socialIdHash) {
        Member member = memberRepository.save(Member.restore(null, provider, socialIdHash, new Nickname("탈퇴회원"),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, 0, false, NOW, null, null,
                NOW, null));
        member.withdraw(NOW);
        return memberRepository.save(member).getId();
    }
}
