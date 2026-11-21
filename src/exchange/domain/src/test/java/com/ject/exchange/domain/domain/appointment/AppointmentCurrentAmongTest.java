package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AppointmentCurrentAmongTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant FUTURE_SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant PAST_SCHEDULED_AT = Instant.parse("2026-10-25T06:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-25T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    @Test
    @DisplayName("약속 목록이 비어 있으면 현재 약속이 없다")
    void currentAmong_empty_returnsEmpty() {
        assertTrue(Appointment.currentAmong(List.of(), NOW).isEmpty());
    }

    @Test
    @DisplayName("PROPOSED이고 만료 시각이 지금보다 뒤면 그 약속이 현재 약속이다")
    void currentAmong_proposedBeforeExpiry_returnsIt() {
        Appointment proposed = appointment(3L, AppointmentStatus.PROPOSED, NOW.plusSeconds(1), FUTURE_SCHEDULED_AT);

        Optional<Appointment> current = Appointment.currentAmong(List.of(proposed), NOW);

        assertEquals(3L, current.orElseThrow().getId());
    }

    @Test
    @DisplayName("CONFIRMED이면 약속 시각이 지났어도 현재 약속이다")
    void currentAmong_confirmedPastSchedule_returnsIt() {
        Appointment confirmed = appointment(3L, AppointmentStatus.CONFIRMED, null, PAST_SCHEDULED_AT);

        Optional<Appointment> current = Appointment.currentAmong(List.of(confirmed), NOW);

        assertEquals(3L, current.orElseThrow().getId());
    }

    @Test
    @DisplayName("PROPOSED이고 만료 시각이 지금과 같으면 현재 약속이 없다")
    void currentAmong_proposedAtExpiry_returnsEmpty() {
        Appointment proposed = appointment(3L, AppointmentStatus.PROPOSED, NOW, FUTURE_SCHEDULED_AT);

        assertTrue(Appointment.currentAmong(List.of(proposed), NOW).isEmpty());
    }

    @Test
    @DisplayName("PROPOSED이고 만료 시각이 지났으면 현재 약속이 없고 저장 상태는 PROPOSED 그대로다")
    void currentAmong_proposedAfterExpiry_returnsEmptyAndKeepsStatus() {
        Appointment proposed = appointment(3L, AppointmentStatus.PROPOSED, NOW.minus(Duration.ofHours(1)),
                FUTURE_SCHEDULED_AT);

        Optional<Appointment> current = Appointment.currentAmong(List.of(proposed), NOW);

        assertTrue(current.isEmpty());
        assertEquals(AppointmentStatus.PROPOSED, proposed.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "EXPIRED", "AUTO_REJECTED",
            "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("끝난 상태의 약속만 있으면 만료 시각이 남았어도 현재 약속이 없다")
    void currentAmong_endedOnly_returnsEmpty(AppointmentStatus status) {
        Appointment ended = appointment(3L, status, NOW.plus(Duration.ofHours(1)), FUTURE_SCHEDULED_AT);

        assertTrue(Appointment.currentAmong(List.of(ended), NOW).isEmpty());
    }

    @Test
    @DisplayName("REJECTED 약속과 만료 전 PROPOSED 약속이 함께 있으면 PROPOSED 약속을 고른다")
    void currentAmong_rejectedThenProposed_returnsProposed() {
        Appointment rejected = appointment(3L, AppointmentStatus.REJECTED, NOW.plus(Duration.ofHours(1)),
                FUTURE_SCHEDULED_AT);
        Appointment proposed = appointment(4L, AppointmentStatus.PROPOSED, NOW.plus(Duration.ofHours(2)),
                FUTURE_SCHEDULED_AT);

        Optional<Appointment> current = Appointment.currentAmong(List.of(rejected, proposed), NOW);

        assertEquals(4L, current.orElseThrow().getId());
    }

    @Test
    @DisplayName("CANCELED 약속과 CONFIRMED 약속이 함께 있으면 CONFIRMED 약속을 고른다")
    void currentAmong_canceledThenConfirmed_returnsConfirmed() {
        Appointment canceled = appointment(3L, AppointmentStatus.CANCELED, null, FUTURE_SCHEDULED_AT);
        Appointment confirmed = appointment(4L, AppointmentStatus.CONFIRMED, null, FUTURE_SCHEDULED_AT);

        Optional<Appointment> current = Appointment.currentAmong(List.of(canceled, confirmed), NOW);

        assertEquals(4L, current.orElseThrow().getId());
    }

    @Test
    @DisplayName("현재 약속이 목록 앞에 있고 지난 약속이 뒤에 있어도 현재 약속을 고른다")
    void currentAmong_currentFirstInList_returnsCurrent() {
        Appointment confirmed = appointment(3L, AppointmentStatus.CONFIRMED, null, FUTURE_SCHEDULED_AT);
        Appointment rejected = appointment(4L, AppointmentStatus.REJECTED, NOW.plus(Duration.ofHours(1)),
                FUTURE_SCHEDULED_AT);

        Optional<Appointment> current = Appointment.currentAmong(List.of(confirmed, rejected), NOW);

        assertEquals(3L, current.orElseThrow().getId());
    }

    private static Appointment appointment(Long id, AppointmentStatus status, Instant expiresAt,
                                           Instant scheduledAt) {
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? NOW.minus(Duration.ofHours(2)) : null;
        return Appointment.of(id, EXCHANGE_ID, 100L, 200L, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt), PLACE,
                new AppointmentItemIds(List.of(31L)), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }
}
