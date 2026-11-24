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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AppointmentRejectTest {

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

    // ---- 거절 (AC-1) ----

    @Test
    @DisplayName("받는 사람이 만료 전 제안을 거절하면 REJECTED인 새 객체이고 나머지 값은 그대로다")
    void reject_byReceiver_changesStatusOnly() {
        // given
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        // when
        Appointment rejected = proposal.reject(OWNER_ID, NOW);

        // then
        assertEquals(AppointmentStatus.REJECTED, rejected.getStatus());
        assertEquals(APPOINTMENT_ID, rejected.getId());
        assertEquals(EXCHANGE_ID, rejected.getExchangeId());
        assertEquals(REQUESTER_ID, rejected.getProposerUserId());
        assertEquals(OWNER_ID, rejected.getReceiverUserId());
        assertEquals(proposal.getTime(), rejected.getTime());
        assertEquals(PLACE, rejected.getPlace());
        assertEquals(proposal.getItemIds(), rejected.getItemIds());
        assertEquals(EXPIRES_AT, rejected.getExpiresAt());
        assertEquals(30, rejected.getProposerReminderOffsetMinutes());
        assertNull(rejected.getReceiverReminderOffsetMinutes());
        assertNull(rejected.getConfirmedAt());
        assertEquals(CREATED_AT, rejected.getCreatedAt());
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("만료 시각 1초 전에 거절하면 REJECTED다")
    void reject_justBeforeExpiry_rejects() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), NOW.plusSeconds(1));

        assertEquals(AppointmentStatus.REJECTED, proposal.reject(OWNER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("만료 전 지금 바로 약속을 거절하면 REJECTED다")
    void reject_nowAppointmentBeforeExpiry_rejects() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.plus(Duration.ofHours(23)));

        assertEquals(AppointmentStatus.REJECTED, proposal.reject(OWNER_ID, NOW).getStatus());
    }

    // ---- 받는 사람 아님 (AC-7) ----

    @Test
    @DisplayName("제안자가 자기 제안을 거절하면 EXCHANGE_104")
    void reject_byProposer_throwsNotReceiver() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, () -> proposal.reject(REQUESTER_ID, NOW));
        assertEquals(AppointmentStatus.PROPOSED, proposal.getStatus());
    }

    @Test
    @DisplayName("받는 사람이 아닌 회원이 거절하면 EXCHANGE_104")
    void reject_byOtherMember_throwsNotReceiver() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, () -> proposal.reject(OTHER_MEMBER_ID, NOW));
    }

    // ---- 진행 중이 아닌 제안 (AC-8) ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "AUTO_REJECTED",
            "CONFIRMED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("저장 상태가 PROPOSED가 아니면 EXCHANGE_106이고 상태는 그대로다")
    void reject_notProposed_throwsProposalNotActive(AppointmentStatus status) {
        Appointment appointment = proposal(status, scheduled(SCHEDULED_AT), EXPIRES_AT);

        assertError(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, () -> appointment.reject(OWNER_ID, NOW));
        assertEquals(status, appointment.getStatus());
    }

    // ---- 자동 만료 없음 (#117) ----

    @Test
    @DisplayName("제안 후 25시간이 지나 옛 만료 시각이 지났어도 REJECTED다")
    void reject_pastOldExpiry_rejects() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), NOW.minus(Duration.ofHours(1)));

        Appointment result = proposal.reject(OWNER_ID, NOW);

        assertEquals(AppointmentStatus.REJECTED, result.getStatus());
    }

    @Test
    @DisplayName("지금 바로 약속이 제안 후 25시간 지났어도 REJECTED다")
    void reject_nowAppointmentPastOldExpiry_rejects() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.minus(Duration.ofMinutes(1)));

        assertEquals(AppointmentStatus.REJECTED, proposal.reject(OWNER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("약속 시각이 이미 지난 제안이어도 REJECTED다")
    void reject_scheduledAtPassed_rejects() {
        Instant oneHourAgo = NOW.minus(Duration.ofHours(1));
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(oneHourAgo), null);

        assertEquals(AppointmentStatus.REJECTED, proposal.reject(OWNER_ID, NOW).getStatus());
    }

    @Test
    @DisplayName("만료 시각이 null인 제안도 REJECTED이고 만료 시각은 null 그대로다")
    void reject_nullExpiresAt_rejects() {
        Appointment proposal = proposal(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), null);

        Appointment result = proposal.reject(OWNER_ID, NOW);

        assertEquals(AppointmentStatus.REJECTED, result.getStatus());
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
