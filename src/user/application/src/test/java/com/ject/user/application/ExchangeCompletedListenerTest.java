package com.ject.user.application;

import com.ject.common.event.exchange.ExchangeCompleted;
import com.ject.user.application.event.ExchangeCompletedListener;
import com.ject.user.application.service.CompletedCountService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.MannerScore;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExchangeCompletedListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");

    @Test
    @DisplayName("교환이 완료되면 두 회원의 교환 완료 횟수가 1씩 늘어난다")
    void increaseBothMembers() {
        // given
        FakeMemberRepository memberRepository = new FakeMemberRepository();
        Long requesterId = save(memberRepository, "요청자", 2);
        Long ownerId = save(memberRepository, "게시자", 0);
        ExchangeCompletedListener listener = listener(memberRepository);

        // when
        listener.handle(new ExchangeCompleted(1L, List.of(requesterId, ownerId), false));

        // then
        assertEquals(3, memberRepository.findById(requesterId).orElseThrow().getCompletedCount());
        assertEquals(1, memberRepository.findById(ownerId).orElseThrow().getCompletedCount());
    }

    @Test
    @DisplayName("[교환했어요] 답으로 완료되면 두 회원의 매너 지표가 2씩 오른다")
    void answeredRaisesMannerScore() {
        // given
        FakeMemberRepository memberRepository = new FakeMemberRepository();
        Long requesterId = save(memberRepository, "요청자", 0);
        Long ownerId = save(memberRepository, "게시자", 0);

        // when
        listener(memberRepository).handle(new ExchangeCompleted(1L, List.of(requesterId, ownerId), false));

        // then
        assertEquals(52, memberRepository.findById(requesterId).orElseThrow().getMannerScore().value());
        assertEquals(52, memberRepository.findById(ownerId).orElseThrow().getMannerScore().value());
    }

    @Test
    @DisplayName("자동 완료 등 [교환했어요] 답이 아닌 완료는 완료 횟수만 오르고 매너 지표는 그대로다")
    void autoCompletedKeepsMannerScore() {
        // given
        FakeMemberRepository memberRepository = new FakeMemberRepository();
        Long requesterId = save(memberRepository, "요청자", 0);
        Long ownerId = save(memberRepository, "게시자", 0);

        // when
        listener(memberRepository).handle(new ExchangeCompleted(1L, List.of(requesterId, ownerId), true));

        // then
        Member requester = memberRepository.findById(requesterId).orElseThrow();
        assertEquals(1, requester.getCompletedCount());
        assertEquals(MannerScore.START, requester.getMannerScore().value());
    }

    @Test
    @DisplayName("횟수를 올리다 실패해도 예외를 넘기지 않는다 (교환은 이미 커밋됨)")
    void failureIsNotPropagated() {
        // given
        FakeMemberRepository memberRepository = new FakeMemberRepository() {
            @Override
            public void increaseCompletedCount(Collection<Long> ids, int mannerPoints) {
                throw new IllegalStateException("DB 오류");
            }
        };
        ExchangeCompletedListener listener = listener(memberRepository);

        // when & then
        assertDoesNotThrow(() -> listener.handle(new ExchangeCompleted(1L, List.of(1L, 2L), false)));
    }

    private static ExchangeCompletedListener listener(FakeMemberRepository memberRepository) {
        return new ExchangeCompletedListener(new CompletedCountService(memberRepository));
    }

    private static Long save(FakeMemberRepository repository, String nickname, int completedCount) {
        Member member = Member.restore(null, SocialProvider.NAVER, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, completedCount, false,
                NOW, null, null, NOW, null);
        return repository.save(member).getId();
    }
}
