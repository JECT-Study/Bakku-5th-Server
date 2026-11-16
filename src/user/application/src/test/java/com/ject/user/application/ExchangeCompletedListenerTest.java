package com.ject.user.application;

import com.ject.common.event.exchange.ExchangeCompleted;
import com.ject.user.application.event.ExchangeCompletedListener;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        ExchangeCompletedListener listener = new ExchangeCompletedListener(memberRepository);

        // when
        listener.handle(new ExchangeCompleted(1L, List.of(requesterId, ownerId), false));

        // then
        assertEquals(3, memberRepository.findById(requesterId).orElseThrow().getCompletedCount());
        assertEquals(1, memberRepository.findById(ownerId).orElseThrow().getCompletedCount());
    }

    private static Long save(FakeMemberRepository repository, String nickname, int completedCount) {
        Member member = Member.restore(null, SocialProvider.NAVER, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, completedCount, false,
                NOW, null, null, NOW, null);
        return repository.save(member).getId();
    }
}
