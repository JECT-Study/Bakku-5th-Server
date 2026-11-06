package com.ject.user.application;

import com.ject.user.application.dto.MemberSummary;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberSummaryTest {

    private static final Instant NOW = Instant.parse("2026-11-06T03:00:00Z");

    @Test
    @DisplayName("이용 중인 회원은 닉네임과 완료 횟수를 그대로 담는다")
    void activeMember() {
        // given
        Member member = restore(MemberStatus.ACTIVE);

        // when
        MemberSummary summary = MemberSummary.from(member);

        // then
        assertEquals(1L, summary.userId());
        assertEquals("바꾸", summary.nickname());
        assertEquals(3, summary.completedCount());
        assertFalse(summary.withdrawn());
    }

    @Test
    @DisplayName("탈퇴한 회원은 닉네임이 비어 있어도 '탈퇴한 사용자'로 표시한다")
    void withdrawnMember() {
        // given
        Member member = restore(MemberStatus.ACTIVE);
        member.withdraw(NOW);

        // when
        MemberSummary summary = MemberSummary.from(member);

        // then
        assertEquals(MemberSummary.WITHDRAWN_NICKNAME, summary.nickname());
        assertTrue(summary.withdrawn());
    }

    @Test
    @DisplayName("영구 정지 회원은 탈퇴한 회원과 같게 표시한다")
    void bannedMember() {
        // when
        MemberSummary summary = MemberSummary.from(restore(MemberStatus.BANNED));

        // then
        assertEquals(MemberSummary.WITHDRAWN_NICKNAME, summary.nickname());
        assertTrue(summary.withdrawn());
    }

    private static Member restore(MemberStatus status) {
        return Member.restore(1L, SocialProvider.KAKAO, "hash", new Nickname("바꾸"),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, 3, false, NOW, null, null, NOW, null);
    }
}
