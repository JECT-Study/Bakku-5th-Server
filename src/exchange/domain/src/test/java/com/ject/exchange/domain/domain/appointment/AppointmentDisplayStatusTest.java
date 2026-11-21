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

import static org.junit.jupiter.api.Assertions.assertEquals;
class AppointmentDisplayStatusTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-25T05:00:00Z");
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    @Test
    @DisplayName("PROPOSED이고 만료 시각이 지금보다 뒤면 보여 줄 상태는 PROPOSED다")
    void displayStatusAt_proposedBeforeExpiry_returnsProposed() {
        Appointment appointment = appointment(AppointmentStatus.PROPOSED, NOW.plusSeconds(1));

        assertEquals(AppointmentStatus.PROPOSED, appointment.displayStatusAt(NOW));
    }

    @Test
    @DisplayName("PROPOSED이고 만료 시각이 지금과 같으면 보여 줄 상태는 EXPIRED다")
    void displayStatusAt_proposedAtExpiry_returnsExpired() {
        Appointment appointment = appointment(AppointmentStatus.PROPOSED, NOW);

        assertEquals(AppointmentStatus.EXPIRED, appointment.displayStatusAt(NOW));
    }

    @Test
    @DisplayName("PROPOSED이고 만료 시각이 지났으면 보여 줄 상태는 EXPIRED다")
    void displayStatusAt_proposedAfterExpiry_returnsExpired() {
        Appointment appointment = appointment(AppointmentStatus.PROPOSED, NOW.minus(Duration.ofHours(1)));

        assertEquals(AppointmentStatus.EXPIRED, appointment.displayStatusAt(NOW));
    }

    @Test
    @DisplayName("만료 시각이 지나 EXPIRED로 보여도 저장 상태는 PROPOSED 그대로다")
    void displayStatusAt_expired_keepsStoredStatus() {
        Appointment appointment = appointment(AppointmentStatus.PROPOSED, NOW.minus(Duration.ofHours(1)));

        appointment.displayStatusAt(NOW);

        assertEquals(AppointmentStatus.PROPOSED, appointment.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROPOSED"})
    @DisplayName("PROPOSED가 아니면 만료 시각이 지났어도 보여 줄 상태는 저장 상태 그대로다")
    void displayStatusAt_notProposed_returnsStoredStatus(AppointmentStatus status) {
        Appointment appointment = appointment(status, NOW.minus(Duration.ofHours(1)));

        assertEquals(status, appointment.displayStatusAt(NOW));
    }

    private static Appointment appointment(AppointmentStatus status, Instant expiresAt) {
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? NOW.minus(Duration.ofHours(2)) : null;
        return Appointment.of(3L, 1L, 100L, 200L, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(31L)), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }
}
