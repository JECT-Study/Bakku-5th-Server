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
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AppointmentChangeReminderTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-27 14:00 (+09:00) */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = NOW.minus(Duration.ofHours(2));
    private static final Instant CONFIRMED_AT = NOW.minus(Duration.ofHours(1));
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    /** 제안자 A (교환 요청자) */
    private static final Long REQUESTER_ID = 100L;
    /** 받는 사람 B (글 게시자) */
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Integer PROPOSER_REMINDER = 30;
    private static final Integer RECEIVER_REMINDER = 60;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    // ---- 제안자 변경 (AC-1) ----

    @Test
    @DisplayName("제안자가 알림을 10으로 바꾸면 제안자 알림만 10인 새 객체이고 나머지 값은 그대로다")
    void changeReminder_byProposer_changesProposerReminderOnly() {
        // given
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT));

        // when
        Appointment changed = confirmed.changeReminder(REQUESTER_ID, 10, NOW);

        // then
        assertEquals(10, changed.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, changed.getReceiverReminderOffsetMinutes());
        assertEquals(AppointmentStatus.CONFIRMED, changed.getStatus());
        assertEquals(APPOINTMENT_ID, changed.getId());
        assertEquals(EXCHANGE_ID, changed.getExchangeId());
        assertEquals(REQUESTER_ID, changed.getProposerUserId());
        assertEquals(OWNER_ID, changed.getReceiverUserId());
        assertEquals(confirmed.getTime(), changed.getTime());
        assertEquals(PLACE, changed.getPlace());
        assertEquals(confirmed.getItemIds(), changed.getItemIds());
        assertEquals(EXPIRES_AT, changed.getExpiresAt());
        assertEquals(CONFIRMED_AT, changed.getConfirmedAt());
        assertEquals(CREATED_AT, changed.getCreatedAt());
    }

    @Test
    @DisplayName("알림을 바꿔도 원래 객체의 제안자 알림은 30 그대로다")
    void changeReminder_returnsNewObject_keepsOriginal() {
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT));

        confirmed.changeReminder(REQUESTER_ID, 10, NOW);

        assertEquals(PROPOSER_REMINDER, confirmed.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, confirmed.getReceiverReminderOffsetMinutes());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 1440})
    @DisplayName("제안자 알림 경계값 10 · 1440이면 그대로 담긴다")
    void changeReminder_boundaryValue_keepsValue(int reminder) {
        Appointment changed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT))
                .changeReminder(REQUESTER_ID, reminder, NOW);

        assertEquals(reminder, changed.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, changed.getReceiverReminderOffsetMinutes());
    }

    // ---- 받는 사람 변경 (AC-2) ----

    @Test
    @DisplayName("받는 사람이 알림을 1440으로 바꾸면 받는 사람 알림만 1440이고 제안자 알림은 30 그대로다")
    void changeReminder_byReceiver_changesReceiverReminderOnly() {
        // given
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT));

        // when
        Appointment changed = confirmed.changeReminder(OWNER_ID, 1440, NOW);

        // then
        assertEquals(1440, changed.getReceiverReminderOffsetMinutes());
        assertEquals(PROPOSER_REMINDER, changed.getProposerReminderOffsetMinutes());
        assertEquals(AppointmentStatus.CONFIRMED, changed.getStatus());
        assertEquals(confirmed.getTime(), changed.getTime());
        assertEquals(PLACE, changed.getPlace());
        assertEquals(confirmed.getItemIds(), changed.getItemIds());
        assertEquals(CONFIRMED_AT, changed.getConfirmedAt());
    }

    // ---- 끄기 (AC-3) ----

    @Test
    @DisplayName("제안자가 알림을 null로 바꾸면 제안자 알림 null이고 받는 사람 알림은 60 그대로다")
    void changeReminder_nullByProposer_turnsOffProposerReminder() {
        Appointment changed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT))
                .changeReminder(REQUESTER_ID, null, NOW);

        assertNull(changed.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, changed.getReceiverReminderOffsetMinutes());
        assertEquals(AppointmentStatus.CONFIRMED, changed.getStatus());
    }

    @Test
    @DisplayName("받는 사람이 알림을 null로 바꾸면 받는 사람 알림 null이고 제안자 알림은 30 그대로다")
    void changeReminder_nullByReceiver_turnsOffReceiverReminder() {
        Appointment changed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT))
                .changeReminder(OWNER_ID, null, NOW);

        assertNull(changed.getReceiverReminderOffsetMinutes());
        assertEquals(PROPOSER_REMINDER, changed.getProposerReminderOffsetMinutes());
    }

    // ---- 알림 시각 경과 (AC-4) ----

    @Test
    @DisplayName("약속 20분 전에 알림 30으로 바꾸면 알림 시각이 지났어도 30이 저장된다")
    void changeReminder_reminderTimeAlreadyPassed_keepsValue() {
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(NOW.plus(Duration.ofMinutes(20))));

        Appointment changed = confirmed.changeReminder(REQUESTER_ID, 30, NOW);

        assertEquals(30, changed.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, changed.getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("약속 1초 전에 알림 1440으로 바꿔도 1440이 저장된다")
    void changeReminder_oneSecondBeforeScheduledAt_keepsValue() {
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(NOW.plusSeconds(1)));

        Appointment changed = confirmed.changeReminder(OWNER_ID, 1440, NOW);

        assertEquals(1440, changed.getReceiverReminderOffsetMinutes());
    }

    // ---- 참여자 아님 (AC-8) ----

    @Test
    @DisplayName("제안자도 받는 사람도 아닌 회원이 바꾸면 EXCHANGE_002이고 두 알림은 그대로다")
    void changeReminder_byOtherMember_throwsNotParticipant() {
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(SCHEDULED_AT));

        assertError(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> confirmed.changeReminder(OTHER_MEMBER_ID, 10, NOW));
        assertEquals(PROPOSER_REMINDER, confirmed.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, confirmed.getReceiverReminderOffsetMinutes());
    }

    // ---- 확정이 아닌 약속 (AC-9) ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PROPOSED", "REJECTED", "WITHDRAWN", "EXPIRED",
            "AUTO_REJECTED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("저장 상태가 CONFIRMED가 아니면 EXCHANGE_109이고 상태와 알림은 그대로다")
    void changeReminder_notConfirmed_throwsInvalidAppointmentStatus(AppointmentStatus status) {
        Appointment appointment = confirmed(status, scheduled(SCHEDULED_AT));

        assertError(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> appointment.changeReminder(REQUESTER_ID, 10, NOW));
        assertEquals(status, appointment.getStatus());
        assertEquals(PROPOSER_REMINDER, appointment.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("만료 전 PROPOSED 제안의 제안자가 바꿔도 EXCHANGE_109")
    void changeReminder_pendingProposalByProposer_throwsInvalidAppointmentStatus() {
        Appointment proposal = appointment(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT), EXPIRES_AT,
                PROPOSER_REMINDER, null, null);

        assertError(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> proposal.changeReminder(REQUESTER_ID, 10, NOW));
    }

    @Test
    @DisplayName("만료된 PROPOSED 제안을 바꾸면 EXCHANGE_109")
    void changeReminder_expiredProposal_throwsInvalidAppointmentStatus() {
        Appointment proposal = appointment(AppointmentStatus.PROPOSED, scheduled(SCHEDULED_AT),
                NOW.minus(Duration.ofMinutes(1)), PROPOSER_REMINDER, null, null);

        assertError(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> proposal.changeReminder(REQUESTER_ID, 10, NOW));
    }

    @Test
    @DisplayName("확정이 아니고 약속 시각도 지났으면 EXCHANGE_109가 먼저다")
    void changeReminder_canceledAndScheduledAtPassed_throwsInvalidAppointmentStatus() {
        Appointment canceled = confirmed(AppointmentStatus.CANCELED, scheduled(NOW.minus(Duration.ofHours(1))));

        assertError(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> canceled.changeReminder(REQUESTER_ID, 10, NOW));
    }

    @Test
    @DisplayName("확정이 아닌 지금 바로 약속이면 EXCHANGE_109가 먼저다")
    void changeReminder_completedNowAppointment_throwsInvalidAppointmentStatus() {
        Appointment completed = appointment(AppointmentStatus.COMPLETED, nowTime(), EXPIRES_AT, null, null,
                CONFIRMED_AT);

        assertError(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> completed.changeReminder(REQUESTER_ID, 30, NOW));
    }

    // ---- 약속 시각 지남 (AC-10) ----

    @Test
    @DisplayName("약속 시각이 지금과 같으면 EXCHANGE_107이고 알림은 그대로다")
    void changeReminder_scheduledAtNow_throwsScheduledAtPassed() {
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(NOW));

        assertError(ExchangeErrorCode.SCHEDULED_AT_PASSED, () -> confirmed.changeReminder(REQUESTER_ID, 10, NOW));
        assertEquals(PROPOSER_REMINDER, confirmed.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("약속 시각이 지났으면 EXCHANGE_107")
    void changeReminder_scheduledAtPassed_throwsScheduledAtPassed() {
        Appointment confirmed = confirmed(AppointmentStatus.CONFIRMED, scheduled(NOW.minus(Duration.ofHours(1))));

        assertError(ExchangeErrorCode.SCHEDULED_AT_PASSED, () -> confirmed.changeReminder(OWNER_ID, null, NOW));
    }

    // ---- 지금 바로 약속 (AC-11) ----

    @Test
    @DisplayName("지금 바로 확정 약속에 알림 30을 주면 EXCHANGE_110이고 알림은 null 그대로다")
    void changeReminder_nowAppointmentWithValue_throwsReminderNotAllowed() {
        Appointment confirmed = appointment(AppointmentStatus.CONFIRMED, nowTime(), EXPIRES_AT, null, null,
                CONFIRMED_AT);

        assertError(ExchangeErrorCode.REMINDER_NOT_ALLOWED, () -> confirmed.changeReminder(REQUESTER_ID, 30, NOW));
        assertNull(confirmed.getProposerReminderOffsetMinutes());
        assertNull(confirmed.getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("지금 바로 확정 약속에 알림 null을 줘도 EXCHANGE_110")
    void changeReminder_nowAppointmentWithNull_throwsReminderNotAllowed() {
        Appointment confirmed = appointment(AppointmentStatus.CONFIRMED, nowTime(), EXPIRES_AT, null, null,
                CONFIRMED_AT);

        assertError(ExchangeErrorCode.REMINDER_NOT_ALLOWED, () -> confirmed.changeReminder(OWNER_ID, null, NOW));
    }

    // ---- helpers ----

    private static void assertError(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    /** A가 제안하고 B가 수락한 약속 P (제안자 알림 30 · 받는 사람 알림 60) */
    private static Appointment confirmed(AppointmentStatus status, AppointmentTime time) {
        return appointment(status, time, EXPIRES_AT, PROPOSER_REMINDER, RECEIVER_REMINDER, CONFIRMED_AT);
    }

    private static Appointment appointment(AppointmentStatus status, AppointmentTime time, Instant expiresAt,
                                           Integer proposerReminder, Integer receiverReminder,
                                           Instant confirmedAt) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status, time, PLACE,
                new AppointmentItemIds(List.of(31L)), expiresAt, proposerReminder, receiverReminder, confirmedAt,
                CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduled(Instant scheduledAt) {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt);
    }

    private static AppointmentTime nowTime() {
        return new AppointmentTime(AppointmentTimeType.NOW, null);
    }
}
