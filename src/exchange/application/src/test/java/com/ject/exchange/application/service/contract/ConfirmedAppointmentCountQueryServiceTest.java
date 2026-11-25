package com.ject.exchange.application.service.contract;

import com.ject.exchange.application.contract.ConfirmedAppointmentCountQuery;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
class ConfirmedAppointmentCountQueryServiceTest {

    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;
    private static final Instant NOW = Instant.parse("2026-11-25T06:00:00Z");
    private static final Place PLACE = new Place("홍대입구역", "서울 마포구", 37.5571, 126.9245);

    private FakeAppointmentRepository appointmentRepository;
    private ConfirmedAppointmentCountQuery query;

    @BeforeEach
    void setUp() {
        appointmentRepository = new FakeAppointmentRepository();
        query = new ConfirmedAppointmentCountQueryService(appointmentRepository);
    }

    @Test
    @DisplayName("확정 약속이 없으면 0이다")
    void countByMemberId_noConfirmedAppointment_returnsZero() {
        assertEquals(0, query.countByMemberId(MEMBER_A));
    }

    @Test
    @DisplayName("제안자로 참여한 확정 약속을 센다")
    void countByMemberId_confirmedAsProposer_countsAppointment() {
        appointmentRepository.preload(appointment(1L, MEMBER_A, MEMBER_B, AppointmentStatus.CONFIRMED));

        assertEquals(1, query.countByMemberId(MEMBER_A));
    }

    @Test
    @DisplayName("받는 사람으로 참여한 확정 약속을 센다")
    void countByMemberId_confirmedAsReceiver_countsAppointment() {
        appointmentRepository.preload(appointment(1L, MEMBER_B, MEMBER_A, AppointmentStatus.CONFIRMED));

        assertEquals(1, query.countByMemberId(MEMBER_A));
    }

    @Test
    @DisplayName("제안자와 받는 사람으로 참여한 확정 약속을 약속별로 합산한다")
    void countByMemberId_confirmedInBothRoles_countsEveryAppointmentOnce() {
        appointmentRepository.preload(
                appointment(1L, MEMBER_A, MEMBER_B, AppointmentStatus.CONFIRMED),
                appointment(2L, MEMBER_A, MEMBER_C, AppointmentStatus.CONFIRMED),
                appointment(3L, MEMBER_B, MEMBER_A, AppointmentStatus.CONFIRMED),
                appointment(4L, MEMBER_B, MEMBER_C, AppointmentStatus.CONFIRMED));

        assertEquals(3, query.countByMemberId(MEMBER_A));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("CONFIRMED가 아닌 약속은 세지 않는다")
    void countByMemberId_nonConfirmedStatus_doesNotCount(AppointmentStatus status) {
        appointmentRepository.preload(appointment(1L, MEMBER_A, MEMBER_B, status));

        assertEquals(0, query.countByMemberId(MEMBER_A));
    }

    @Test
    @DisplayName("회원 ID가 null이면 저장소를 조회하지 않고 0이다")
    void countByMemberId_null_returnsZeroWithoutRepositoryCall() {
        assertEquals(0, query.countByMemberId(null));
        assertEquals(0, appointmentRepository.countCalls());
    }

    @Test
    @DisplayName("반복 조회해도 같은 건수를 반환하고 약속 상태는 바뀌지 않는다")
    void countByMemberId_repeatedQuery_doesNotChangeAppointment() {
        Appointment confirmed = appointment(1L, MEMBER_A, MEMBER_B, AppointmentStatus.CONFIRMED);
        appointmentRepository.preload(confirmed);

        assertEquals(1, query.countByMemberId(MEMBER_A));
        assertEquals(1, query.countByMemberId(MEMBER_A));
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
    }

    private static Appointment appointment(Long id, Long proposerId, Long receiverId, AppointmentStatus status) {
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? NOW : null;
        return Appointment.of(id, id, proposerId, receiverId, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, NOW.plusSeconds(3600)), PLACE,
                new AppointmentItemIds(List.of(id)), null, 30, 30, confirmedAt, NOW, NOW);
    }
}
