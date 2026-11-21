package com.ject.user.domain;

import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberAlreadyWithdrawnException;
import com.ject.user.domain.exception.UnderAgeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    private static final Instant NOW = Instant.parse("2026-11-06T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test
    @DisplayName("가입하면 ACTIVE 상태이고 완료 횟수는 0, 닉네임 변경 시각은 비어 있다")
    void create() {
        // when
        Member member = signup(LocalDate.of(2000, 1, 1), Gender.FEMALE);

        // then
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        assertEquals(0, member.getCompletedCount());
        assertNull(member.getNicknameChangedAt());
        assertEquals(NOW, member.getCreatedAt());
        assertEquals(NOW, member.getMarketingChangedAt());
    }

    @Test
    @DisplayName("성별은 입력하지 않아도 가입할 수 있다")
    void createWithoutGender() {
        // when
        Member member = signup(LocalDate.of(2000, 1, 1), null);

        // then
        assertNull(member.getGender());
    }

    @Test
    @DisplayName("만 14세 미만은 가입할 수 없다")
    void createUnderAge() {
        assertThrows(UnderAgeException.class, () -> signup(LocalDate.of(2012, 11, 7), null));
    }

    @Test
    @DisplayName("만 14세 판정은 전달받은 오늘 날짜 기준이다")
    void ageUsesGivenDate() {
        // given: 14번째 생일이 11/6
        BirthDate birthDate = new BirthDate(LocalDate.of(2012, 11, 6));

        // when & then
        assertThrows(UnderAgeException.class, () -> Member.create(SocialProvider.KAKAO, "hash", new Nickname("바꾸"),
                birthDate, null, false, null, NOW, LocalDate.of(2026, 11, 5)));
        assertDoesNotThrow(() -> Member.create(SocialProvider.KAKAO, "hash", new Nickname("바꾸"), birthDate,
                null, false, null, NOW, LocalDate.of(2026, 11, 6)));
    }

    @Test
    @DisplayName("탈퇴하면 닉네임 · 생년월일 · 성별을 비우고 소셜 식별값 해시는 남긴다")
    void withdraw() {
        // given
        Member member = signup(LocalDate.of(2000, 1, 1), Gender.MALE);
        Instant withdrawnAt = NOW.plusSeconds(60);

        // when
        member.withdraw(withdrawnAt);

        // then
        assertTrue(member.isWithdrawn());
        assertNull(member.getNickname());
        assertNull(member.getBirthDate());
        assertNull(member.getGender());
        assertEquals("hash", member.getSocialIdHash());
        assertEquals(withdrawnAt, member.getWithdrawnAt());
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
    void withdrawTwice() {
        // given
        Member member = signup(LocalDate.of(2000, 1, 1), null);
        member.withdraw(NOW);

        // when & then
        assertThrows(MemberAlreadyWithdrawnException.class, () -> member.withdraw(NOW));
    }

    @Test
    @DisplayName("교환 완료 횟수는 1씩 늘어난다")
    void increaseCompletedCount() {
        // given
        Member member = signup(LocalDate.of(2000, 1, 1), null);

        // when
        member.increaseCompletedCount();
        member.increaseCompletedCount();

        // then
        assertEquals(2, member.getCompletedCount());
    }

    @Test
    @DisplayName("영구 정지 회원은 탈퇴하지 않았지만 상대에게는 탈퇴한 회원으로 보인다")
    void bannedAppearsWithdrawn() {
        // given
        Member banned = restoreWithStatus(MemberStatus.BANNED);
        Member restricted = restoreWithStatus(MemberStatus.RESTRICTED);

        // when & then
        assertFalse(banned.isWithdrawn());
        assertTrue(banned.appearsWithdrawn());
        assertFalse(restricted.appearsWithdrawn());
    }

    @Test
    @DisplayName("탈퇴 시각 + 7일부터 같은 소셜 계정으로 다시 가입할 수 있다")
    void canRejoinAfter7Days() {
        // given
        Member member = restoreWithStatus(MemberStatus.ACTIVE);
        member.withdraw(NOW);
        Instant rejoinAt = NOW.plus(Duration.ofDays(7));

        // when & then
        assertEquals(rejoinAt, member.rejoinAvailableAt());
        assertFalse(member.canRejoin(rejoinAt.minusSeconds(1)));
        assertTrue(member.canRejoin(rejoinAt));
    }

    @Test
    @DisplayName("탈퇴하지 않은 회원은 재가입 시각이 없다")
    void rejoinAvailableAtRequiresWithdrawal() {
        // given
        Member member = restoreWithStatus(MemberStatus.ACTIVE);

        // when & then
        assertThrows(IllegalStateException.class, member::rejoinAvailableAt);
    }

    @Test
    @DisplayName("마케팅 수신 동의를 바꾸면 값과 변경 일시가 바뀐다")
    void changeMarketingConsent() {
        // given
        Member member = signup(LocalDate.of(2000, 1, 1), null);
        Instant later = NOW.plus(Duration.ofDays(1));

        // when
        member.changeMarketingConsent(false, later);

        // then
        assertFalse(member.isMarketingAgreed());
        assertEquals(later, member.getMarketingChangedAt());
    }

    @Test
    @DisplayName("마케팅 수신 동의를 같은 값으로 보내면 변경 일시는 그대로다")
    void changeMarketingConsent_sameValue() {
        // given
        Member member = signup(LocalDate.of(2000, 1, 1), null);

        // when
        member.changeMarketingConsent(true, NOW.plus(Duration.ofDays(1)));

        // then
        assertTrue(member.isMarketingAgreed());
        assertEquals(NOW, member.getMarketingChangedAt());
    }

    @Test
    @DisplayName("닉네임을 바꾼 적이 없으면 지금 바꿀 수 있다")
    void nicknameChangeableAt_neverChanged() {
        // given
        Member member = signup(LocalDate.of(2000, 1, 1), null);

        // when & then
        assertNull(member.nicknameChangeableAt(NOW));
    }

    @Test
    @DisplayName("닉네임을 바꾸면 30일 뒤부터 다시 바꿀 수 있다")
    void nicknameChangeableAt_after30Days() {
        // given
        Instant changedAt = NOW;
        Instant changeableAt = changedAt.plus(Duration.ofDays(30));
        Member member = Member.restore(1L, SocialProvider.NAVER, "hash", new Nickname("바꾸"),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, MemberStatus.ACTIVE, null, 0, false, NOW, changedAt,
                null, NOW, null);

        // when & then
        assertEquals(changeableAt, member.nicknameChangeableAt(changeableAt.minusSeconds(1)));
        assertNull(member.nicknameChangeableAt(changeableAt));
    }

    private static Member signup(LocalDate birthDate, Gender gender) {
        return Member.create(SocialProvider.KAKAO, "hash", new Nickname("바꾸"), new BirthDate(birthDate),
                gender, true, null, NOW, TODAY);
    }

    private static Member restoreWithStatus(MemberStatus status) {
        return Member.restore(1L, SocialProvider.NAVER, "hash", new Nickname("바꾸"),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, 3, false, NOW, null, null, NOW, null);
    }
}
