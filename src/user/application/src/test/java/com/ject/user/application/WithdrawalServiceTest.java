package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.member.MemberWithdrawn;
import com.ject.user.application.port.WithdrawalReasonRepository;
import com.ject.user.application.service.WithdrawalService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.DevicePlatform;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.PushDevice;
import com.ject.user.domain.domain.RefreshToken;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.domain.WithdrawalReason;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawalServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T03:00:00Z");
    private static final Instant JOINED_AT = Instant.parse("2026-11-01T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;
    private FakePushDeviceRepository pushDeviceRepository;
    private FakeWithdrawalReasonRepository withdrawalReasonRepository;
    private List<Object> publishedEvents;
    private WithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
        pushDeviceRepository = new FakePushDeviceRepository();
        withdrawalReasonRepository = new FakeWithdrawalReasonRepository();
        publishedEvents = new ArrayList<>();
        withdrawalService = new WithdrawalService(memberRepository, refreshTokenRepository, pushDeviceRepository,
                withdrawalReasonRepository, publishedEvents::add,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("MB-6 탈퇴하면 개인정보를 비우고 WITHDRAWN으로 바꾼다")
    void withdraw() {
        // given
        Long memberId = saveMember("쿠로미덕후");

        // when
        withdrawalService.withdraw(memberId, null);

        // then
        Member member = memberRepository.findById(memberId).orElseThrow();
        assertEquals(MemberStatus.WITHDRAWN, member.getStatus());
        assertEquals(NOW, member.getWithdrawnAt());
        assertNull(member.getNickname());
        assertNull(member.getBirthDate());
        assertNull(member.getGender());
    }

    @Test
    @DisplayName("MB-6 탈퇴하면 이 회원의 모든 기기 리프레시 · 푸시 토큰을 지우고 다른 회원 것은 남긴다")
    void withdraw_deletesTokensOfMemberOnly() {
        // given
        Long memberId = saveMember("쿠로미덕후");
        Long otherId = saveMember("하치와레러버");
        refreshTokenRepository.save(RefreshToken.issue(memberId, "phone", "hash-1", NOW));
        refreshTokenRepository.save(RefreshToken.issue(memberId, "tablet", "hash-2", NOW));
        refreshTokenRepository.save(RefreshToken.issue(otherId, "other-phone", "hash-3", NOW));
        pushDeviceRepository.save(PushDevice.register(memberId, "phone", "fcm-1", DevicePlatform.IOS, "1.0.0"));
        pushDeviceRepository.save(PushDevice.register(otherId, "other-phone", "fcm-2", DevicePlatform.IOS, "1.0.0"));

        // when
        withdrawalService.withdraw(memberId, null);

        // then
        assertEquals(1, refreshTokenRepository.count());
        assertEquals(1, pushDeviceRepository.count());
        assertTrue(pushDeviceRepository.findByDeviceId("other-phone").isPresent());
    }

    @Test
    @DisplayName("MB-6 탈퇴 이벤트를 발행한다")
    void withdraw_publishesEvent() {
        // given
        Long memberId = saveMember("쿠로미덕후");

        // when
        withdrawalService.withdraw(memberId, null);

        // then
        assertEquals(List.of(new MemberWithdrawn(memberId, NOW)), publishedEvents);
    }

    @Test
    @DisplayName("MB-6 사유를 고르면 저장하고, 고르지 않으면 저장하지 않는다")
    void withdraw_savesReasonOnlyWhenGiven() {
        // given
        Long withReason = saveMember("쿠로미덕후");
        Long withoutReason = saveMember("하치와레러버");

        // when
        withdrawalService.withdraw(withReason, "HARD_TO_FIND_PARTNER");
        withdrawalService.withdraw(withoutReason, null);

        // then
        assertEquals(List.of(new SavedReason(withReason, WithdrawalReason.HARD_TO_FIND_PARTNER, NOW)),
                withdrawalReasonRepository.saved);
    }

    @Test
    @DisplayName("MB-6 이미 탈퇴한 회원이면 MemberNotFoundException, 이벤트는 다시 발행하지 않는다")
    void withdraw_alreadyWithdrawn() {
        // given
        Long memberId = saveMember("쿠로미덕후");
        withdrawalService.withdraw(memberId, null);
        publishedEvents.clear();

        // when & then
        assertThrows(MemberNotFoundException.class, () -> withdrawalService.withdraw(memberId, null));
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @DisplayName("MB-6 없는 회원이면 MemberNotFoundException")
    void withdraw_unknownMember() {
        assertThrows(MemberNotFoundException.class, () -> withdrawalService.withdraw(999L, null));
    }

    private Long saveMember(String nickname) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2001, 3, 15)), Gender.FEMALE, MemberStatus.ACTIVE, null, 0, false,
                JOINED_AT, null, null, JOINED_AT, null);
        return memberRepository.save(member).getId();
    }

    private record SavedReason(Long memberId, WithdrawalReason reason, Instant withdrawnAt) {
    }

    private static class FakeWithdrawalReasonRepository implements WithdrawalReasonRepository {

        private final List<SavedReason> saved = new ArrayList<>();

        @Override
        public void save(Long memberId, WithdrawalReason reason, Instant withdrawnAt) {
            saved.add(new SavedReason(memberId, reason, withdrawnAt));
        }
    }
}
