package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AppointmentAcceptTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-27 14:00 (+09:00) */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = NOW.minus(Duration.ofHours(1));
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    // ---- 수락 ----

    @Test
    @DisplayName("받는 사람이 수락하면 CONFIRMED · 확정 시각 now · 받는 사람 알림 30이고 나머지 값은 그대로다")
    void accept_byReceiver_confirms() {
        // given
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        // when
        Appointment accepted = proposal.accept(OWNER_ID, NOW);

        // then
        assertEquals(AppointmentStatus.CONFIRMED, accepted.getStatus());
        assertEquals(NOW, accepted.getConfirmedAt());
        assertEquals(APPOINTMENT_ID, accepted.getId());
        assertEquals(EXCHANGE_ID, accepted.getExchangeId());
        assertEquals(REQUESTER_ID, accepted.getProposerUserId());
        assertEquals(OWNER_ID, accepted.getReceiverUserId());
        assertEquals(proposal.getTime(), accepted.getTime());
        assertEquals(PLACE, accepted.getPlace());
        assertEquals(List.of(31L), accepted.getItemIds().values());
        assertEquals(30, accepted.getProposerReminderOffsetMinutes());
        assertEquals(30, accepted.getReceiverReminderOffsetMinutes());
        assertEquals(CREATED_AT, accepted.getCreatedAt());
    }

    @Test
    @DisplayName("수락은 새 객체를 반환하고 원래 제안은 PROPOSED 그대로다")
    void accept_returnsNewObject_keepsOriginal() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        proposal.accept(OWNER_ID, NOW);

        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
        assertNull(proposal.getConfirmedAt());
    }

    @Test
    @DisplayName("약속 시각이 20분 뒤라 알림 시각이 이미 지났어도 받는 사람 알림은 30으로 저장된다")
    void accept_reminderAlreadyPassed_keepsThirty() {
        Instant twentyMinutesLater = NOW.plus(Duration.ofMinutes(20));
        Appointment accepted = proposal(AppointmentStatus.PROPOSED, scheduled(twentyMinutesLater), null)
                .accept(OWNER_ID, NOW);

        assertEquals(30, accepted.getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("제안자 알림이 30이 아닌 옛 제안을 수락해도 받는 사람 알림은 30이고 제안자 알림은 그대로다")
    void accept_legacyProposerReminder_setsReceiverThirtyOnly() {
        Appointment legacy = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), PLACE, new AppointmentItemIds(List.of(31L)),
                EXPIRES_AT, 60, null, null, CREATED_AT, CREATED_AT);

        Appointment accepted = legacy.accept(OWNER_ID, NOW);

        assertEquals(30, accepted.getReceiverReminderOffsetMinutes());
        assertEquals(60, accepted.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("지금 바로 약속을 수락하면 CONFIRMED이고 두 사람 알림은 null이다")
    void accept_nowAppointment_noReminder() {
        Appointment nowProposal = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null), PLACE,
                new AppointmentItemIds(List.of(31L)), null, null, null, null, CREATED_AT, CREATED_AT);

        Appointment accepted = nowProposal.accept(OWNER_ID, NOW);

        assertEquals(AppointmentStatus.CONFIRMED, accepted.getStatus());
        assertNull(accepted.getReceiverReminderOffsetMinutes());
        assertNull(accepted.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("만료 시각 1초 전이면 수락된다")
    void accept_justBeforeExpiry_confirms() {
        Instant expiresAt = NOW.plusSeconds(1);
        Appointment accepted = proposal(AppointmentStatus.PROPOSED, scheduled(expiresAt), expiresAt)
                .accept(OWNER_ID, NOW);

        assertEquals(AppointmentStatus.CONFIRMED, accepted.getStatus());
    }

    // ---- 받는 사람 확인 ----

    @Test
    @DisplayName("제안자가 수락하면 EXCHANGE_104")
    void accept_byProposer_throwsNotReceiver() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, () -> proposal.accept(REQUESTER_ID, NOW));
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("받는 사람 확인에서 제안자면 EXCHANGE_104")
    void validateReceiver_proposer_throwsNotReceiver() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, () -> proposal.validateReceiver(REQUESTER_ID));
    }

    @Test
    @DisplayName("받는 사람 확인에서 제3자면 EXCHANGE_104")
    void validateReceiver_thirdParty_throwsNotReceiver() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, () -> proposal.validateReceiver(OTHER_MEMBER_ID));
    }

    @Test
    @DisplayName("받는 사람 확인에서 받는 사람이면 통과한다")
    void validateReceiver_receiver_passes() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertDoesNotThrow(() -> proposal.validateReceiver(OWNER_ID));
    }

    // ---- 제안 상태 ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
            names = {"REJECTED", "WITHDRAWN", "CONFIRMED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("진행 중이 아닌 제안을 수락하면 EXCHANGE_106이고 상태는 그대로다")
    void accept_notProposed_throwsProposalNotActive(AppointmentStatus status) {
        Appointment appointment = proposal(status, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, () -> appointment.accept(OWNER_ID, NOW));
        assertEquals(status, appointment.getStatus());
    }

    @Test
    @DisplayName("자동 거절된 제안을 수락하면 EXCHANGE_108")
    void accept_autoRejected_throwsPostReservedElsewhere() {
        Appointment appointment = proposal(AppointmentStatus.AUTO_REJECTED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.POST_RESERVED_ELSEWHERE, () -> appointment.accept(OWNER_ID, NOW));
        assertEquals(AppointmentStatus.AUTO_REJECTED, appointment.getStatus());
    }

    // ---- 시각 ----

    @Test
    @DisplayName("약속 시각이 지금과 같으면 EXCHANGE_107이고 PROPOSED 그대로다")
    void accept_scheduledAtNow_throwsScheduledAtPassed() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(NOW), null);

        assertError(ExchangeErrorCode.SCHEDULED_AT_PASSED, () -> proposal.accept(OWNER_ID, NOW));
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("약속 시각이 지났으면 EXCHANGE_107")
    void accept_scheduledAtPassed_throwsScheduledAtPassed() {
        Instant oneHourAgo = NOW.minus(Duration.ofHours(1));
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(oneHourAgo), oneHourAgo);

        assertError(ExchangeErrorCode.SCHEDULED_AT_PASSED, () -> proposal.accept(OWNER_ID, NOW));
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("약속 시각 1초 전이면 수락된다")
    void accept_oneSecondBeforeScheduledAt_confirms() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(NOW.plusSeconds(1)), null);

        assertEquals(AppointmentStatus.CONFIRMED, proposal.accept(OWNER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("옛 만료 시각이 지금과 같고 약속 시각은 뒤면 수락된다")
    void accept_oldExpiryNowBeforeScheduledAt_confirms() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(NOW.plus(Duration.ofHours(1))), NOW);

        assertEquals(AppointmentStatus.CONFIRMED, proposal.accept(OWNER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("제안 후 25시간이 지나 옛 만료 시각이 지났고 약속 시각은 뒤면 수락된다")
    void accept_pastOldExpiryBeforeScheduledAt_confirms() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT),
                NOW.minus(Duration.ofHours(1)));

        Appointment accepted = proposal.accept(OWNER_ID, NOW);

        assertEquals(AppointmentStatus.CONFIRMED, accepted.getStatus());
        assertEquals(30, accepted.getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("지금 바로 약속이 제안 후 25시간 지났어도 수락된다")
    void accept_nowAppointmentPastOldExpiry_confirms() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.NOW, null), NOW.minus(Duration.ofHours(1)));

        assertEquals(AppointmentStatus.CONFIRMED, proposal.accept(OWNER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("제안자가 약속 시각이 지난 제안을 수락해도 EXCHANGE_104가 먼저다")
    void accept_byProposerAfterScheduledAt_throwsNotReceiver() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(NOW), NOW);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, () -> proposal.accept(REQUESTER_ID, NOW));
    }

    @Test
    @DisplayName("약속 시각이 지난 거절된 제안을 수락하면 EXCHANGE_106이 먼저다")
    void accept_rejectedPastSchedule_throwsProposalNotActive() {
        Appointment proposal = proposal(AppointmentStatus.REJECTED, scheduled(NOW.minus(Duration.ofHours(1))), null);

        assertError(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, () -> proposal.accept(OWNER_ID, NOW));
    }

    // ---- 자동 거절 ----

    @Test
    @DisplayName("자동 거절하면 AUTO_REJECTED인 새 객체이고 나머지 값은 그대로다")
    void autoReject_changesStatusOnly() {
        // given
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        // when
        Appointment rejected = proposal.autoReject();

        // then
        assertEquals(AppointmentStatus.AUTO_REJECTED, rejected.getStatus());
        assertEquals(APPOINTMENT_ID, rejected.getId());
        assertEquals(EXCHANGE_ID, rejected.getExchangeId());
        assertEquals(REQUESTER_ID, rejected.getProposerUserId());
        assertEquals(OWNER_ID, rejected.getReceiverUserId());
        assertEquals(proposal.getTime(), rejected.getTime());
        assertEquals(PLACE, rejected.getPlace());
        assertEquals(proposal.getItemIds(), rejected.getItemIds());
        assertEquals(EXPIRES_AT, rejected.getExpiresAt());
        assertEquals(30, rejected.getProposerReminderOffsetMinutes());
        assertNull(rejected.getConfirmedAt());
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    // ---- helpers ----

    private static void assertError(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    private static Appointment proposal(AppointmentStatus status, AppointmentTime time, Instant expiresAt) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status, time, PLACE,
                new AppointmentItemIds(List.of(31L)), expiresAt, 30, null, null, CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduled(Instant scheduledAt) {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt);
    }
}
