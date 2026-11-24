package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetCurrentAppointmentQuery;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class GetCurrentAppointmentHandlerTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant PAST_SCHEDULED_AT = Instant.parse("2026-10-25T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T04:50:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-26T04:55:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long LATER_APPOINTMENT_ID = 5L;
    private static final Long OTHER_APPOINTMENT_ID = 4L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long MISSING_EXCHANGE_ID = 999L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    private static final Long SOURCE_POST_31 = 501L;
    private static final String THUMBNAIL_31 = "https://cdn.example.com/post/501/1.jpg";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private GetCurrentAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        FakePostPort postPort = new FakePostPort();
        handler = new GetCurrentAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_31, "우사기 미니 피규어", THUMBNAIL_31, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("만료 전 PROPOSED 약속이 있으면 제안자 조회에 약속 값 · 만료 시각 · 내 알림 값을 담아 반환한다")
    void handle_pendingProposalByProposer_returnsAppointment() {
        // given
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, SCHEDULED_AT));

        // when
        Optional<AppointmentResult> current = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID,
                REQUESTER_ID));

        // then
        AppointmentResult result = current.orElseThrow();
        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(EXCHANGE_ID, result.exchangeId());
        assertEquals(REQUESTER_ID, result.proposerUserId());
        assertEquals(OWNER_ID, result.receiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertEquals(AppointmentTimeType.SCHEDULED, result.timeType());
        assertEquals(SCHEDULED_AT, result.scheduledAt());
        assertEquals(PLACE, result.place());
        assertEquals(List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL_31)),
                result.items());
        assertNull(result.expiresAt());
        assertEquals(30, result.myReminderOffsetMinutes());
        assertNull(result.confirmedAt());
        assertEquals(CREATED_AT, result.createdAt());
    }

    @Test
    @DisplayName("받는 사람이 조회하면 같은 약속을 반환하고 내 알림 값은 null이다")
    void handle_pendingProposalByReceiver_returnsSameAppointmentWithNullReminder() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, OWNER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(REQUESTER_ID, result.proposerUserId());
        assertEquals(OWNER_ID, result.receiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertEquals(SCHEDULED_AT, result.scheduledAt());
        assertEquals(PLACE, result.place());
        assertNull(result.expiresAt());
        assertNull(result.myReminderOffsetMinutes());
    }

    @Test
    @DisplayName("CONFIRMED 약속이면 expiresAt null · confirmedAt 확정 시각으로 반환한다")
    void handle_confirmed_returnsConfirmedAppointment() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                EXPIRES_AT, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertNull(result.expiresAt());
        assertEquals(CONFIRMED_AT, result.confirmedAt());
    }

    @Test
    @DisplayName("CONFIRMED 약속은 약속 시각이 지났어도 반환한다")
    void handle_confirmedPastSchedule_returnsAppointment() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                null, PAST_SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertEquals(PAST_SCHEDULED_AT, result.scheduledAt());
    }

    // ---- 현재 약속 없음 ----

    @Test
    @DisplayName("교환에 약속이 없으면 같은 글의 다른 교환에 진행 중 제안이 있어도 빈 결과다")
    void handle_noAppointment_returnsEmptyIgnoringOtherExchange() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        exchangeRepository.preload(exchange(OTHER_EXCHANGE_ID, OTHER_MEMBER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, EXPIRES_AT, SCHEDULED_AT));

        Optional<AppointmentResult> current = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID,
                REQUESTER_ID));

        assertTrue(current.isEmpty());
    }

    @Test
    @DisplayName("PROPOSED 약속이 제안 후 25시간 지나 옛 만료 시각이 지났어도 그 제안을 PROPOSED · 만료 시각 null로 반환한다")
    void handle_pastOldExpiryProposal_returnsIt() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                NOW.minus(Duration.ofHours(1)), SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertNull(result.expiresAt());
        assertEquals(0, appointmentRepository.saveCount());
    }

    @Test
    @DisplayName("PROPOSED 약속의 옛 만료 시각이 지금과 같아도 그 제안을 반환한다")
    void handle_proposalAtOldExpiry_returnsIt() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                NOW, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
    }

    @Test
    @DisplayName("PROPOSED 약속의 약속 시각이 지났어도 그 제안을 PROPOSED로 반환한다")
    void handle_proposalPastSchedule_returnsIt() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                null, NOW.minus(Duration.ofHours(1))));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "AUTO_REJECTED",
            "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("끝난 상태의 약속만 있으면 빈 결과다")
    void handle_endedOnly_returnsEmpty(AppointmentStatus status) {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, status, EXPIRES_AT, SCHEDULED_AT));

        Optional<AppointmentResult> current = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID,
                REQUESTER_ID));

        assertTrue(current.isEmpty());
    }

    // ---- 여러 약속 ----

    @Test
    @DisplayName("먼저 거절된 약속과 나중 제안이 함께 있으면 나중 제안을 반환한다")
    void handle_rejectedThenProposed_returnsProposed() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.REJECTED,
                EXPIRES_AT, SCHEDULED_AT));
        appointmentRepository.preload(appointment(LATER_APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(LATER_APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
    }

    @Test
    @DisplayName("취소된 약속과 확정된 약속이 함께 있으면 확정된 약속을 반환한다")
    void handle_canceledThenConfirmed_returnsConfirmed() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CANCELED,
                null, SCHEDULED_AT));
        appointmentRepository.preload(appointment(LATER_APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                null, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(LATER_APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
    }

    // ---- 실패 · 권한 ----

    @Test
    @DisplayName("없는 교환이면 EXCHANGE_NOT_FOUND")
    void handle_missingExchange_throwsExchangeNotFound() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetCurrentAppointmentQuery(MISSING_EXCHANGE_ID, REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.EXCHANGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 조회하면 NOT_EXCHANGE_PARTICIPANT")
    void handle_thirdParty_throwsNotExchangeParticipant() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, SCHEDULED_AT));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    @Test
    @DisplayName("참여자가 아니면 교환에 약속이 없어도 NOT_EXCHANGE_PARTICIPANT")
    void handle_thirdPartyWithoutAppointment_throwsNotExchangeParticipant() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    // ---- 다른 방 진행 중 제안 ----

    @Test
    @DisplayName("같은 글의 다른 교환에 만료 전 PROPOSED가 있으면 otherActiveProposalsOnPost가 true다")
    void handle_pendingProposalInOtherExchange_returnsTrue() {
        givenCurrentAndOtherExchange();
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, NOW.plus(Duration.ofHours(1)), SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertTrue(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환에 제안이 없으면 otherActiveProposalsOnPost가 false다")
    void handle_noOtherProposal_returnsFalse() {
        givenCurrentAndOtherExchange();

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertFalse(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환의 PROPOSED가 옛 만료 시각을 지났어도 otherActiveProposalsOnPost가 true다")
    void handle_pastOldExpiryProposalInOtherExchange_returnsTrue() {
        givenCurrentAndOtherExchange();
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, NOW.minus(Duration.ofMinutes(10)), SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertTrue(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환의 PROPOSED 옛 만료 시각이 지금과 같아도 otherActiveProposalsOnPost가 true다")
    void handle_proposalAtOldExpiryInOtherExchange_returnsTrue() {
        givenCurrentAndOtherExchange();
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, NOW, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertTrue(result.otherActiveProposalsOnPost());
    }

    // ---- 종료된 교환 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 종료 상태여도 CONFIRMED 약속이 남아 있으면 반환한다")
    void handle_endedExchangeWithConfirmed_returnsAppointment(ExchangeStatus exchangeStatus) {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, exchangeStatus));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                null, SCHEDULED_AT));

        AppointmentResult result = handler.handle(new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID))
                .orElseThrow();

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
    }

    // ---- helpers ----

    private void givenCurrentAndOtherExchange() {
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
        exchangeRepository.preload(exchange(OTHER_EXCHANGE_ID, OTHER_MEMBER_ID, ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, SCHEDULED_AT));
    }

    private static Exchange exchange(Long exchangeId, Long requesterId, ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(exchangeId, POST_ID, requesterId, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0)),
                status, completedAt, NOW, NOW);
    }

    /** 이 교환의 약속은 요청자가 제안했고, 다른 교환의 약속은 그 교환의 요청자가 제안했다. */
    private static Appointment appointment(Long id, Long exchangeId, AppointmentStatus status, Instant expiresAt,
                                           Instant scheduledAt) {
        Long proposerId = exchangeId.equals(EXCHANGE_ID) ? REQUESTER_ID : OTHER_MEMBER_ID;
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? CONFIRMED_AT : null;
        return Appointment.of(id, exchangeId, proposerId, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }
}
