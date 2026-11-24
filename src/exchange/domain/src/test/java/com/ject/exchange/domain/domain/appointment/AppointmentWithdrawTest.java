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
class AppointmentWithdrawTest {

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

    // ---- 철회 (AC-1) ----

    @Test
    @DisplayName("제안자가 만료 전 제안을 철회하면 WITHDRAWN인 새 객체이고 나머지 값은 그대로다")
    void withdraw_byProposer_changesStatusOnly() {
        // given
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        // when
        Appointment withdrawn = proposal.withdraw(REQUESTER_ID, NOW);

        // then
        assertEquals(AppointmentStatus.WITHDRAWN, withdrawn.getStatus());
        assertEquals(APPOINTMENT_ID, withdrawn.getId());
        assertEquals(EXCHANGE_ID, withdrawn.getExchangeId());
        assertEquals(REQUESTER_ID, withdrawn.getProposerUserId());
        assertEquals(OWNER_ID, withdrawn.getReceiverUserId());
        assertEquals(proposal.getTime(), withdrawn.getTime());
        assertEquals(PLACE, withdrawn.getPlace());
        assertEquals(proposal.getItemIds(), withdrawn.getItemIds());
        assertEquals(EXPIRES_AT, withdrawn.getExpiresAt());
        assertEquals(30, withdrawn.getProposerReminderOffsetMinutes());
        assertNull(withdrawn.getReceiverReminderOffsetMinutes());
        assertNull(withdrawn.getConfirmedAt());
        assertEquals(CREATED_AT, withdrawn.getCreatedAt());
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("만료 시각 1초 전에 철회하면 WITHDRAWN이다")
    void withdraw_justBeforeExpiry_withdraws() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), NOW.plusSeconds(1));

        assertEquals(AppointmentStatus.WITHDRAWN, proposal.withdraw(REQUESTER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("만료 전 지금 바로 약속을 철회하면 WITHDRAWN이다")
    void withdraw_nowAppointmentBeforeExpiry_withdraws() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.plus(Duration.ofHours(23)));

        assertEquals(AppointmentStatus.WITHDRAWN, proposal.withdraw(REQUESTER_ID, NOW).getStatus());
    }

    // ---- 제안자 확인 (AC-7) ----

    @Test
    @DisplayName("제안자면 validateProposer는 예외가 없다")
    void validateProposer_byProposer_passes() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertDoesNotThrow(() -> proposal.validateProposer(REQUESTER_ID));
    }

    @Test
    @DisplayName("받는 사람이면 validateProposer는 EXCHANGE_105")
    void validateProposer_byReceiver_throwsNotProposer() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER, () -> proposal.validateProposer(OWNER_ID));
    }

    @Test
    @DisplayName("제안자가 아닌 다른 회원이면 validateProposer는 EXCHANGE_105")
    void validateProposer_byOtherMember_throwsNotProposer() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER, () -> proposal.validateProposer(OTHER_MEMBER_ID));
    }

    @Test
    @DisplayName("받는 사람이 철회하면 EXCHANGE_105이고 PROPOSED 그대로다")
    void withdraw_byReceiver_throwsNotProposer() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER, () -> proposal.withdraw(OWNER_ID, NOW));
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("제안자가 아닌 다른 회원이 철회하면 EXCHANGE_105")
    void withdraw_byOtherMember_throwsNotProposer() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER, () -> proposal.withdraw(OTHER_MEMBER_ID, NOW));
    }

    // ---- 진행 중이 아닌 제안 (AC-8) ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "AUTO_REJECTED",
            "CONFIRMED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("저장 상태가 PROPOSED가 아니면 EXCHANGE_106이고 상태는 그대로다")
    void withdraw_notProposed_throwsProposalNotActive(AppointmentStatus status) {
        Appointment appointment = proposal(status, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, () -> appointment.withdraw(REQUESTER_ID, NOW));
        assertEquals(status, appointment.getStatus());
    }

    // ---- 자동 만료 없음 (#117) ----

    @Test
    @DisplayName("제안 후 25시간이 지나 옛 만료 시각이 지났어도 WITHDRAWN다")
    void withdraw_pastOldExpiry_withdraws() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), NOW.minus(Duration.ofHours(1)));

        Appointment result = proposal.withdraw(REQUESTER_ID, NOW);

        assertEquals(AppointmentStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    @DisplayName("지금 바로 약속이 제안 후 25시간 지났어도 WITHDRAWN다")
    void withdraw_nowAppointmentPastOldExpiry_withdraws() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.minus(Duration.ofMinutes(1)));

        assertEquals(AppointmentStatus.WITHDRAWN, proposal.withdraw(REQUESTER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("약속 시각이 이미 지난 제안이어도 WITHDRAWN다")
    void withdraw_scheduledAtPassed_withdraws() {
        Instant oneHourAgo = NOW.minus(Duration.ofHours(1));
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(oneHourAgo), null);

        assertEquals(AppointmentStatus.WITHDRAWN, proposal.withdraw(REQUESTER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("만료 시각이 null인 제안도 WITHDRAWN이고 만료 시각은 null 그대로다")
    void withdraw_nullExpiresAt_withdraws() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), null);

        Appointment result = proposal.withdraw(REQUESTER_ID, NOW);

        assertEquals(AppointmentStatus.WITHDRAWN, result.getStatus());
        assertNull(result.getExpiresAt());
    }

    // ---- helpers ----

    private static void assertError(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    private static Appointment proposal(AppointmentStatus status, AppointmentTime time, Instant expiresAt) {
        boolean confirmedBefore = status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELED || status == AppointmentStatus.NOT_MET;
        Instant confirmedAt = confirmedBefore ? CREATED_AT : null;
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status, time, PLACE,
                new AppointmentItemIds(List.of(31L)), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduled(Instant scheduledAt) {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt);
    }
}
