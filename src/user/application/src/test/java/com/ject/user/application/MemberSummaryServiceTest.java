package com.ject.user.application;

import com.ject.user.application.dto.MemberSummary;
import com.ject.user.application.service.MemberSummaryService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberSummaryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private MemberSummaryService memberSummaryService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        memberSummaryService = new MemberSummaryService(memberRepository);
    }

    @Test
    @DisplayName("여러 회원의 요약을 한 번에 조회하고, 탈퇴 회원은 '탈퇴한 사용자'로 준다")
    void getSummaries() {
        // given
        Long activeId = save("바꾸", MemberStatus.ACTIVE);
        Long withdrawnId = save("떠난회원", MemberStatus.ACTIVE);
        Member withdrawn = memberRepository.findById(withdrawnId).orElseThrow();
        withdrawn.withdraw(NOW);
        memberRepository.save(withdrawn);

        // when
        Map<Long, MemberSummary> summaries = memberSummaryService.getSummaries(List.of(activeId, withdrawnId))
                .stream()
                .collect(Collectors.toMap(MemberSummary::userId, Function.identity()));

        // then
        assertEquals("바꾸", summaries.get(activeId).nickname());
        assertEquals(MemberSummary.WITHDRAWN_NICKNAME, summaries.get(withdrawnId).nickname());
        assertTrue(summaries.get(withdrawnId).withdrawn());
    }

    @Test
    @DisplayName("같은 회원 id가 여러 번 와도 한 번만 준다")
    void duplicatedIds() {
        // given
        Long id = save("바꾸", MemberStatus.ACTIVE);

        // when
        List<MemberSummary> summaries = memberSummaryService.getSummaries(List.of(id, id));

        // then
        assertEquals(1, summaries.size());
    }

    @Test
    @DisplayName("빈 목록이면 빈 목록을 준다")
    void emptyIds() {
        // when
        List<MemberSummary> summaries = memberSummaryService.getSummaries(List.of());

        // then
        assertTrue(summaries.isEmpty());
    }

    @Test
    @DisplayName("없는 회원 id가 있으면 MEMBER_201 예외를 던진다")
    void missingMember() {
        // given
        Long id = save("바꾸", MemberStatus.ACTIVE);

        // when · then
        assertThrows(MemberNotFoundException.class, () -> memberSummaryService.getSummaries(List.of(id, 999L)));
    }

    private Long save(String nickname, MemberStatus status) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, 0, false, NOW, null, null, NOW, null);
        return memberRepository.save(member).getId();
    }
}
