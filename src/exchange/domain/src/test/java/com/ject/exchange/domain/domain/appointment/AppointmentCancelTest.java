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

/**
 * E-07-1 약속 취소 도메인 규칙 (#113). 상태 · 참여자 · 교환 확인 열림 시각 · 임박 여부를 본다.
 */
class AppointmentCancelTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 15:30 (+09:00) 교환 확인 열림 시각 */
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    /** 2026-11-23 14:00 (+09:00) 임박 시작 시각 (약속 1시간 전) */
    private static final Instant IMMINENT_FROM = SCHEDULED_AT.minus(Duration.ofHours(1));
    /** 2026-11-23 10:00 (+09:00) 임박 전 평범한 취소 시각 */
    private static final Instant EARLY = Instant.parse("2026-11-23T01:00:00Z");
    /** 2026-11-22 17:00 (+09:00) SCHEDULED 약속 확정 시각 */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    /** 2026-11-23 10:00 (+09:00) NOW 약속 확정 시각 */
    private static final Instant NOW_CONFIRMED_AT = Instant.parse("2026-11-23T01:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    /** 제안자 A */
    private static final Long PROPOSER_ID = 100L;
    /** 받는 사람 B */
    private static final Long RECEIVER_ID = 200L;
    /** 참여자가 아닌 C */
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    // ---- AC-1 제안자가 취소 ----

    @Test
    @DisplayName("제안자가 취소하면 CANCELED · 취소한 사람 제안자 · 결과 시각 now다")
    void cancel_byProposer_cancels() {
        // when
        Appointment canceled = confirmedScheduled().cancel(PROPOSER_ID, EARLY);

        // then
        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
        assertEquals(PROPOSER_ID, canceled.getCanceledByUserId());
        assertEquals(EARLY, canceled.getResolvedAt());
    }

    @Test
    @DisplayName("취소해도 시간 · 장소 · 아이템 · 알림 · 확정 시각 · 참여자는 그대로다")
    void cancel_keepsOtherFields() {
        Appointment confirmed = confirmedScheduled();

        Appointment canceled = confirmed.cancel(PROPOSER_ID, EARLY);

        assertEquals(APPOINTMENT_ID, canceled.getId());
        assertEquals(EXCHANGE_ID, canceled.getExchangeId());
        assertEquals(PROPOSER_ID, canceled.getProposerUserId());
        assertEquals(RECEIVER_ID, canceled.getReceiverUserId());
        assertEquals(confirmed.getTime(), canceled.getTime());
        assertEquals(PLACE, canceled.getPlace());
        assertEquals(List.of(31L), canceled.getItemIds().values());
        assertEquals(30, canceled.getProposerReminderOffsetMinutes());
        assertEquals(60, canceled.getReceiverReminderOffsetMinutes());
        assertEquals(CONFIRMED_AT, canceled.getConfirmedAt());
        assertEquals(CREATED_AT, canceled.getCreatedAt());
    }

    @Test
    @DisplayName("취소는 새 객체를 반환하고 원래 약속은 CONFIRMED · 취소 정보 없음 그대로다")
    void cancel_returnsNewObject_keepsOriginal() {
        Appointment confirmed = confirmedScheduled();

        confirmed.cancel(PROPOSER_ID, EARLY);

        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
        assertNull(confirmed.getResolvedAt());
        assertNull(confirmed.getCanceledByUserId());
        assertNull(confirmed.getCancelImminent());
    }

    // ---- AC-2 받은 사람이 취소 ----

    @Test
    @DisplayName("받는 사람이 취소해도 상대 동의 없이 CANCELED이고 취소한 사람은 받는 사람이다")
    void cancel_byReceiver_cancels() {
        Appointment canceled = confirmedScheduled().cancel(RECEIVER_ID, EARLY);

        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
        assertEquals(RECEIVER_ID, canceled.getCanceledByUserId());
        assertEquals(EARLY, canceled.getResolvedAt());
    }

    // ---- AC-4 임박 여부 기록 ----

    @Test
    @DisplayName("SCHEDULED 15:00 약속을 13:59:59에 취소하면 임박 false다")
    void cancel_oneSecondBeforeImminent_notImminent() {
        Appointment canceled = confirmedScheduled().cancel(PROPOSER_ID, IMMINENT_FROM.minusSeconds(1));

        assertEquals(Boolean.FALSE, canceled.getCancelImminent());
    }

    @Test
    @DisplayName("SCHEDULED 15:00 약속을 14:00:00 정각에 취소하면 임박 true다")
    void cancel_exactlyOneHourBefore_imminent() {
        Appointment canceled = confirmedScheduled().cancel(PROPOSER_ID, IMMINENT_FROM);

        assertEquals(Boolean.TRUE, canceled.getCancelImminent());
    }

    @Test
    @DisplayName("약속 시각이 지난 15:10에 취소하면 임박 true다")
    void cancel_afterScheduledBeforeOpen_imminent() {
        Appointment canceled = confirmedScheduled().cancel(RECEIVER_ID, SCHEDULED_AT.plus(Duration.ofMinutes(10)));

        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
        assertEquals(Boolean.TRUE, canceled.getCancelImminent());
    }

    @Test
    @DisplayName("임박 전 취소도 임박 여부는 null이 아니라 false로 저장된다")
    void cancel_early_recordsFalseNotNull() {
        Appointment canceled = confirmedScheduled().cancel(PROPOSER_ID, EARLY);

        assertEquals(Boolean.FALSE, canceled.getCancelImminent());
    }

    @Test
    @DisplayName("NOW 약속은 확정 직후에 취소해도 임박 true다")
    void cancel_nowAppointmentRightAfterConfirm_imminent() {
        Appointment canceled = confirmedNow().cancel(PROPOSER_ID, NOW_CONFIRMED_AT);

        assertEquals(Boolean.TRUE, canceled.getCancelImminent());
    }

    @Test
    @DisplayName("NOW 약속을 확정 29분 59초 뒤에 취소해도 임박 true다")
    void cancel_nowAppointmentJustBeforeOpen_imminent() {
        Appointment canceled = confirmedNow().cancel(RECEIVER_ID,
                NOW_CONFIRMED_AT.plus(Duration.ofMinutes(30)).minusSeconds(1));

        assertEquals(Boolean.TRUE, canceled.getCancelImminent());
    }

    // ---- AC-5 참여자 아님 ----

    @Test
    @DisplayName("제안자 · 받는 사람이 아닌 회원이 취소하면 EXCHANGE_002다")
    void cancel_byThirdParty_throwsNotParticipant() {
        Appointment confirmed = confirmedScheduled();

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, () -> confirmed.cancel(OTHER_MEMBER_ID, EARLY));
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
    }

    // ---- AC-7 확정이 아닌 약속 ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CONFIRMED")
    @DisplayName("CONFIRMED가 아닌 약속을 취소하면 EXCHANGE_109다")
    void cancel_notConfirmed_throwsInvalidStatus(AppointmentStatus status) {
        Appointment appointment = inStatus(status);

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, () -> appointment.cancel(PROPOSER_ID, EARLY));
        assertEquals(status, appointment.getStatus());
    }

    @Test
    @DisplayName("이미 취소된 약속을 상대가 다시 취소하면 EXCHANGE_109다")
    void cancel_twice_throwsInvalidStatus() {
        Appointment canceled = confirmedScheduled().cancel(PROPOSER_ID, EARLY);

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> canceled.cancel(RECEIVER_ID, EARLY.plusSeconds(1)));
        assertEquals(PROPOSER_ID, canceled.getCanceledByUserId());
    }

    // ---- AC-8 교환 확인이 열린 뒤 ----

    @Test
    @DisplayName("SCHEDULED 15:00 약속은 15:29:59에 취소할 수 있다")
    void cancel_scheduledJustBeforeOpen_cancels() {
        Appointment canceled = confirmedScheduled().cancel(PROPOSER_ID, OPENS_AT.minusSeconds(1));

        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
    }

    @Test
    @DisplayName("SCHEDULED 15:00 약속을 15:30:00 정각에 취소하면 EXCHANGE_207이다")
    void cancel_scheduledAtOpen_throwsCancelAfterOpen() {
        Appointment confirmed = confirmedScheduled();

        assertRejected(ExchangeErrorCode.CANCEL_AFTER_CONFIRMATION_OPEN, () -> confirmed.cancel(PROPOSER_ID, OPENS_AT));
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
    }

    @Test
    @DisplayName("교환 확인이 열리고 한참 뒤에 취소해도 EXCHANGE_207이다")
    void cancel_scheduledLongAfterOpen_throwsCancelAfterOpen() {
        assertRejected(ExchangeErrorCode.CANCEL_AFTER_CONFIRMATION_OPEN,
                () -> confirmedScheduled().cancel(RECEIVER_ID, OPENS_AT.plus(Duration.ofDays(1))));
    }

    @Test
    @DisplayName("NOW 약속은 확정 10:00이면 10:29:59에 취소할 수 있다")
    void cancel_nowJustBeforeOpen_cancels() {
        Appointment canceled = confirmedNow().cancel(PROPOSER_ID,
                NOW_CONFIRMED_AT.plus(Duration.ofMinutes(30)).minusSeconds(1));

        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
    }

    @Test
    @DisplayName("NOW 약속은 확정 10:00이면 10:30:00 정각에 취소하면 EXCHANGE_207이다")
    void cancel_nowAtOpen_throwsCancelAfterOpen() {
        Appointment confirmed = confirmedNow();

        assertRejected(ExchangeErrorCode.CANCEL_AFTER_CONFIRMATION_OPEN,
                () -> confirmed.cancel(PROPOSER_ID, NOW_CONFIRMED_AT.plus(Duration.ofMinutes(30))));
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
    }

    // ---- 복원 팩토리 (AC-13 조회 · 저장 복원) ----

    @Test
    @DisplayName("18개 인자 복원 팩토리는 취소한 사람 · 임박 여부를 그대로 담는다")
    void of_withCancelFields_restoresCancelInfo() {
        Appointment restored = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID,
                AppointmentStatus.CANCELED, scheduled(), PLACE, new AppointmentItemIds(List.of(31L)), EXPIRES_AT,
                30, 60, CONFIRMED_AT, EARLY, List.of(), CREATED_AT, CREATED_AT, RECEIVER_ID, true);

        assertEquals(AppointmentStatus.CANCELED, restored.getStatus());
        assertEquals(RECEIVER_ID, restored.getCanceledByUserId());
        assertEquals(Boolean.TRUE, restored.getCancelImminent());
        assertEquals(EARLY, restored.getResolvedAt());
    }

    @Test
    @DisplayName("16개 · 14개 인자 복원 팩토리는 취소한 사람 · 임박 여부가 null이다")
    void of_legacyFactories_haveNoCancelInfo() {
        Appointment sixteen = confirmedScheduled();
        Appointment fourteen = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID,
                AppointmentStatus.CONFIRMED, scheduled(), PLACE, new AppointmentItemIds(List.of(31L)), EXPIRES_AT,
                30, 60, CONFIRMED_AT, CREATED_AT, CREATED_AT);

        assertNull(sixteen.getCanceledByUserId());
        assertNull(sixteen.getCancelImminent());
        assertNull(fourteen.getCanceledByUserId());
        assertNull(fourteen.getCancelImminent());
    }

    // ---- helpers ----

    private static void assertRejected(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    /** A가 제안하고 B가 수락한 SCHEDULED 15:00 약속 (제안자 알림 30 · 받는 사람 알림 60) */
    private static Appointment confirmedScheduled() {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID, AppointmentStatus.CONFIRMED,
                scheduled(), PLACE, new AppointmentItemIds(List.of(31L)), EXPIRES_AT, 30, 60, CONFIRMED_AT,
                null, List.of(), CREATED_AT, CREATED_AT);
    }

    /** 10:00에 확정된 지금 바로 약속 */
    private static Appointment confirmedNow() {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.NOW, null), PLACE, new AppointmentItemIds(List.of(31L)),
                EXPIRES_AT, 30, 60, NOW_CONFIRMED_AT, null, List.of(), CREATED_AT, CREATED_AT);
    }

    /** 확정 시각 · 결과 시각은 그 상태에 올 수 있는 값으로 채운다 */
    private static Appointment inStatus(AppointmentStatus status) {
        boolean wasConfirmed = status == AppointmentStatus.COMPLETED || status == AppointmentStatus.NOT_MET
                || status == AppointmentStatus.CANCELED;
        Instant confirmedAt = wasConfirmed ? CONFIRMED_AT : null;
        Instant resolvedAt = status == AppointmentStatus.PROPOSED ? null : EARLY.minus(Duration.ofHours(1));
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID, status, scheduled(), PLACE,
                new AppointmentItemIds(List.of(31L)), EXPIRES_AT, 30, 60, confirmedAt, resolvedAt, List.of(),
                CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduled() {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT);
    }
}
