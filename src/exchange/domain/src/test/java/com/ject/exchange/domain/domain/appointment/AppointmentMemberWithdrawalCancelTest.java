package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("139-withdrawn-appointment-cancel-event")
class AppointmentMemberWithdrawalCancelTest {

    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant WITHDRAWN_AT = SCHEDULED_AT.plus(Duration.ofHours(2));
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long PROPOSER_ID = 100L;
    private static final Long RECEIVER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Place PLACE = new Place(
            "홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-3")
    @DisplayName("PROPOSED 약속의 제안자가 탈퇴하면 CANCELED와 탈퇴 시각을 기록한다")
    void cancelByMemberWithdrawal_proposedByProposer_cancels() {
        Appointment proposed = appointmentIn(AppointmentStatus.PROPOSED);

        Appointment canceled = proposed.cancelByMemberWithdrawal(PROPOSER_ID, WITHDRAWN_AT);

        assertCanceled(canceled, PROPOSER_ID);
        assertNull(canceled.getConfirmedAt());
        assertNull(canceled.confirmationOpensAt());
        assertEquals(AppointmentStatus.PROPOSED, proposed.getStatus());
        assertNull(proposed.getResolvedAt());
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-2")
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-4")
    @DisplayName("CONFIRMED 약속의 받는 사람이 탈퇴하면 매너 영향 없이 CANCELED다")
    void cancelByMemberWithdrawal_confirmedByReceiver_cancelsWithoutImminentPenalty() {
        Appointment canceled = appointmentIn(AppointmentStatus.CONFIRMED)
                .cancelByMemberWithdrawal(RECEIVER_ID, WITHDRAWN_AT);

        assertCanceled(canceled, RECEIVER_ID);
        assertEquals(Boolean.FALSE, canceled.getCancelImminent());
        assertEquals(SCHEDULED_AT.plus(Duration.ofMinutes(30)), canceled.confirmationOpensAt());
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-7")
    @DisplayName("교환 확인 질문이 열린 뒤 탈퇴해도 확정 약속은 취소된다")
    void cancelByMemberWithdrawal_afterConfirmationOpen_cancels() {
        Appointment confirmed = appointmentIn(AppointmentStatus.CONFIRMED);

        Appointment canceled = confirmed.cancelByMemberWithdrawal(PROPOSER_ID, WITHDRAWN_AT);

        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
        assertEquals(Boolean.FALSE, canceled.getCancelImminent());
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-2")
    @DisplayName("약속 참여자가 아닌 회원의 탈퇴로 취소하면 EXCHANGE_002다")
    void cancelByMemberWithdrawal_byThirdParty_throwsNotParticipant() {
        Appointment confirmed = appointmentIn(AppointmentStatus.CONFIRMED);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> confirmed.cancelByMemberWithdrawal(OTHER_MEMBER_ID, WITHDRAWN_AT));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"PROPOSED", "CONFIRMED"})
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-8")
    @DisplayName("PROPOSED와 CONFIRMED가 아닌 약속은 탈퇴로 다시 취소하지 않는다")
    void cancelByMemberWithdrawal_finishedStatus_throwsInvalidStatus(AppointmentStatus status) {
        Appointment appointment = appointmentIn(status);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> appointment.cancelByMemberWithdrawal(PROPOSER_ID, WITHDRAWN_AT));

        assertEquals(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, exception.getErrorCode());
        assertEquals(status, appointment.getStatus());
    }

    private static void assertCanceled(Appointment canceled, Long withdrawnMemberId) {
        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());
        assertEquals(withdrawnMemberId, canceled.getCanceledByUserId());
        assertEquals(WITHDRAWN_AT, canceled.getResolvedAt());
        assertEquals(Boolean.FALSE, canceled.getCancelImminent());
    }

    private static Appointment appointmentIn(AppointmentStatus status) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(31L)), null, 30, 60,
                status == AppointmentStatus.PROPOSED ? null : CONFIRMED_AT,
                null, List.of(), CREATED_AT, CREATED_AT, null, null);
    }
}
