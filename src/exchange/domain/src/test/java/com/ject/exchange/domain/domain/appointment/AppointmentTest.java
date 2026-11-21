package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AppointmentTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant TOMORROW_15 = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-27 14:00 (+09:00) = NOW + 24h */
    private static final Instant NOW_PLUS_24H = Instant.parse("2026-10-27T05:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    // ---- 제안 ----

    @Test
    @DisplayName("시간 지정 제안이면 PROPOSED이고 입력 값과 만료 시각(제안 + 24시간)이 담긴다")
    void propose_scheduled_buildsProposedAppointment() {
        // when
        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                scheduled(TOMORROW_15), PLACE, itemIds(), 30, NOW);

        // then
        assertNull(appointment.getId());
        assertNull(appointment.getCreatedAt());
        assertEquals(EXCHANGE_ID, appointment.getExchangeId());
        assertEquals(REQUESTER_ID, appointment.getProposerUserId());
        assertEquals(OWNER_ID, appointment.getReceiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, appointment.getStatus());
        assertEquals(AppointmentTimeType.SCHEDULED, appointment.getTime().type());
        assertEquals(TOMORROW_15, appointment.getTime().scheduledAt());
        assertEquals(PLACE, appointment.getPlace());
        assertEquals(List.of(31L), appointment.getItemIds().values());
        assertEquals(NOW_PLUS_24H, appointment.getExpiresAt());
        assertEquals(30, appointment.getProposerReminderOffsetMinutes());
        assertNull(appointment.getReceiverReminderOffsetMinutes());
        assertNull(appointment.getConfirmedAt());
    }

    @Test
    @DisplayName("약속 시각이 제안 + 24시간보다 빠르면 만료 시각은 약속 시각이다")
    void propose_scheduledBeforeDayLater_expiresAtScheduledAt() {
        Instant oneHourLater = Instant.parse("2026-10-26T06:00:00Z");

        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                scheduled(oneHourLater), PLACE, itemIds(), 30, NOW);

        assertEquals(oneHourLater, appointment.getExpiresAt());
    }

    @Test
    @DisplayName("약속 시각이 정확히 제안 + 24시간이면 만료 시각도 그 시각이다")
    void propose_scheduledExactlyDayLater_expiresAtSameInstant() {
        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                scheduled(NOW_PLUS_24H), PLACE, itemIds(), 30, NOW);

        assertEquals(NOW_PLUS_24H, appointment.getExpiresAt());
    }

    @Test
    @DisplayName("지금 바로 제안이면 만료 시각은 제안 + 24시간이고 알림 값은 버린다")
    void propose_now_expiresAfterDayAndDropsReminder() {
        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                new AppointmentTime(AppointmentTimeType.NOW, null), PLACE, itemIds(), 30, NOW);

        assertEquals(AppointmentStatus.PROPOSED, appointment.getStatus());
        assertEquals(AppointmentTimeType.NOW, appointment.getTime().type());
        assertNull(appointment.getTime().scheduledAt());
        assertEquals(NOW_PLUS_24H, appointment.getExpiresAt());
        assertNull(appointment.getProposerReminderOffsetMinutes());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 1440})
    @DisplayName("알림 값 10 · 1440이면 제안자 알림 값으로 그대로 저장된다")
    void propose_boundaryReminder_keepsValue(int reminder) {
        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                scheduled(TOMORROW_15), PLACE, itemIds(), reminder, NOW);

        assertEquals(reminder, appointment.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("알림 값 null이면 알림 끔으로 저장된다")
    void propose_nullReminder_keepsNull() {
        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                scheduled(TOMORROW_15), PLACE, itemIds(), null, NOW);

        assertNull(appointment.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("알림 시각이 이미 지났어도(1시간 뒤 약속 · 1440분 전) 값은 그대로 저장된다")
    void propose_reminderAlreadyPassed_keepsValue() {
        Appointment appointment = Appointment.propose(EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                scheduled(Instant.parse("2026-10-26T06:00:00Z")), PLACE, itemIds(), 1440, NOW);

        assertEquals(1440, appointment.getProposerReminderOffsetMinutes());
    }

    // ---- 진행 중 판단 ----

    @Test
    @DisplayName("만료 전 PROPOSED는 진행 중이다")
    void isInProgressAt_unexpiredProposed_isTrue() {
        Appointment appointment = stored(AppointmentStatus.PROPOSED, NOW.plus(Duration.ofMinutes(10)));

        assertTrue(appointment.isInProgressAt(NOW));
    }

    @Test
    @DisplayName("CONFIRMED는 진행 중이다")
    void isInProgressAt_confirmed_isTrue() {
        Appointment appointment = stored(AppointmentStatus.CONFIRMED, null);

        assertTrue(appointment.isInProgressAt(NOW));
    }

    @Test
    @DisplayName("만료 시각이 지난 PROPOSED는 진행 중이 아니다")
    void isInProgressAt_expiredProposed_isFalse() {
        Appointment appointment = stored(AppointmentStatus.PROPOSED, NOW.minus(Duration.ofMinutes(1)));

        assertFalse(appointment.isInProgressAt(NOW));
    }

    @Test
    @DisplayName("만료 시각이 지금과 같은 PROPOSED는 진행 중이 아니다")
    void isInProgressAt_expiresNow_isFalse() {
        Appointment appointment = stored(AppointmentStatus.PROPOSED, NOW);

        assertFalse(appointment.isInProgressAt(NOW));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PROPOSED", "CONFIRMED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("PROPOSED · CONFIRMED가 아닌 약속은 진행 중이 아니다")
    void isInProgressAt_otherStatus_isFalse(AppointmentStatus status) {
        Appointment appointment = stored(status, NOW.plus(Duration.ofHours(1)));

        assertFalse(appointment.isInProgressAt(NOW));
    }

    // ---- 다른 방 진행 중 제안 판단 ----

    @Test
    @DisplayName("만료 전 PROPOSED는 대기 중 제안이다")
    void isPendingProposalAt_unexpiredProposed_isTrue() {
        Appointment appointment = stored(AppointmentStatus.PROPOSED, NOW.plus(Duration.ofMinutes(10)));

        assertTrue(appointment.isPendingProposalAt(NOW));
    }

    @Test
    @DisplayName("만료 시각이 지났거나 지금과 같은 PROPOSED는 대기 중 제안이 아니다")
    void isPendingProposalAt_expiredProposed_isFalse() {
        assertFalse(stored(AppointmentStatus.PROPOSED, NOW).isPendingProposalAt(NOW));
        assertFalse(stored(AppointmentStatus.PROPOSED, NOW.minus(Duration.ofMinutes(1))).isPendingProposalAt(NOW));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PROPOSED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("PROPOSED가 아니면(확정 포함) 대기 중 제안이 아니다")
    void isPendingProposalAt_notProposed_isFalse(AppointmentStatus status) {
        Appointment appointment = stored(status, NOW.plus(Duration.ofHours(1)));

        assertFalse(appointment.isPendingProposalAt(NOW));
    }

    // ---- 내 알림 값 ----

    @Test
    @DisplayName("제안자에게는 제안자 알림 값 · 받는 사람에게는 받는 사람 알림 값 · 그 외에는 null")
    void reminderOffsetMinutesOf_returnsValuePerParticipant() {
        Appointment appointment = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, scheduled(TOMORROW_15), PLACE, itemIds(), NOW_PLUS_24H,
                30, 60, null, NOW, NOW);

        assertEquals(30, appointment.reminderOffsetMinutesOf(REQUESTER_ID));
        assertEquals(60, appointment.reminderOffsetMinutesOf(OWNER_ID));
        assertNull(appointment.reminderOffsetMinutesOf(OTHER_MEMBER_ID));
    }

    private static AppointmentItemIds itemIds() {
        return new AppointmentItemIds(List.of(31L));
    }

    private static AppointmentTime scheduled(Instant scheduledAt) {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt);
    }

    private static Appointment stored(AppointmentStatus status, Instant expiresAt) {
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? NOW.minus(Duration.ofHours(1)) : null;
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                scheduled(TOMORROW_15), PLACE, itemIds(), expiresAt, 30, null, confirmedAt,
                NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(2)));
    }
}
