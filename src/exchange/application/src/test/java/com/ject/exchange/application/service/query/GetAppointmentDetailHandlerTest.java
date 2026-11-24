package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A-2 약속 상세 조회. 공통 Given은 ACTIVE 교환(요청자 A · 게시자 B)에서 A가 제안한 SCHEDULED · PROPOSED 약속이다.
 */
class GetAppointmentDetailHandlerTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T04:50:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-26T04:55:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long OTHER_APPOINTMENT_ID = 4L;
    private static final Long MISSING_APPOINTMENT_ID = 999L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long OTHER_REQUESTER_ID = 400L;
    private static final Long ITEM_31 = 31L;
    private static final Long SOURCE_POST_31 = 501L;
    private static final String THUMBNAIL_31 = "https://cdn.example.com/post/501/1.jpg";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private GetAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        FakePostPort postPort = new FakePostPort();
        handler = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_31, "우사기 미니 피규어", THUMBNAIL_31, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
    }

    // ---- AC-1 · AC-2 참여자 조회 ----

    @Test
    @DisplayName("제안자가 조회하면 저장된 약속 값 · 만료 시각 · 내 알림 30을 반환한다")
    void handle_byProposer_returnsFullResult() {
        // given
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);

        // when
        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        // then
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
        assertEquals(CREATED_AT, result.createdAt());
        assertNull(result.confirmedAt());
    }

    @Test
    @DisplayName("받는 사람이 조회하면 같은 약속 정보이고 내 알림은 null이다")
    void handle_byReceiver_returnsSameAppointmentWithNullReminder() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(REQUESTER_ID, result.proposerUserId());
        assertEquals(OWNER_ID, result.receiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertEquals(SCHEDULED_AT, result.scheduledAt());
        assertEquals(PLACE, result.place());
        assertNull(result.expiresAt());
        assertNull(result.myReminderOffsetMinutes());
    }

    // ---- AC-3 · AC-4 실패 ----

    @Test
    @DisplayName("없는 약속이면 APPOINTMENT_NOT_FOUND")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetAppointmentQuery(MISSING_APPOINTMENT_ID, REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 없는 약속을 조회해도 APPOINTMENT_NOT_FOUND가 먼저다")
    void handle_missingAppointmentByThirdParty_throwsAppointmentNotFound() {
        givenExchange(ExchangeStatus.ACTIVE);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetAppointmentQuery(MISSING_APPOINTMENT_ID, OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("교환 참여자가 아닌 회원이 조회하면 NOT_EXCHANGE_PARTICIPANT")
    void handle_thirdParty_throwsNotExchangeParticipant() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    @Test
    @DisplayName("종료된 교환이어도 참여자가 아니면 NOT_EXCHANGE_PARTICIPANT")
    void handle_thirdPartyOnClosedExchange_throwsNotExchangeParticipant() {
        givenExchange(ExchangeStatus.CLOSED);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    // ---- 자동 만료 없음 (#117) ----

    @Test
    @DisplayName("제안 후 25시간이 지나 옛 만료 시각이 지난 PROPOSED면 PROPOSED · 만료 시각 null이다")
    void handle_proposedPastOldExpiry_returnsProposedWithNullExpiresAt() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, NOW.minus(Duration.ofHours(1)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertNull(result.expiresAt());
    }

    @Test
    @DisplayName("옛 만료 시각이 지금과 같은 PROPOSED면 PROPOSED · 만료 시각 null이다")
    void handle_proposedAtOldExpiry_returnsProposed() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, NOW);

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertNull(result.expiresAt());
    }

    @Test
    @DisplayName("만료 시각이 남은 PROPOSED도 만료 시각 null이다")
    void handle_proposedBeforeOldExpiry_returnsNullExpiresAt() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, NOW.plusSeconds(1));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertNull(result.expiresAt());
    }

    @Test
    @DisplayName("약속 시각이 지난 PROPOSED면 PROPOSED다")
    void handle_proposedPastSchedule_returnsProposed() {
        givenExchange(ExchangeStatus.ACTIVE);
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.SCHEDULED,
                        NOW.minus(Duration.ofHours(1))), PLACE, new AppointmentItemIds(List.of(ITEM_31)), null, 30,
                null, null, CREATED_AT, CREATED_AT));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.PROPOSED, result.status());
    }

    @Test
    @DisplayName("옛 만료 시각이 지난 제안을 조회해도 저장 상태는 PROPOSED 그대로이고 저장하지 않는다")
    void handle_proposedPastExpiry_doesNotChangeStoredStatus() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, NOW.minus(Duration.ofMinutes(10)));

        handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(AppointmentStatus.PROPOSED,
                appointmentRepository.findById(APPOINTMENT_ID).orElseThrow().getStatus());
    }

    // ---- AC-7 확정 ----

    @Test
    @DisplayName("확정된 약속이면 CONFIRMED · 만료 시각 null · 확정 시각을 반환한다")
    void handle_confirmed_returnsConfirmedAt() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.CONFIRMED, EXPIRES_AT);

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertNull(result.expiresAt());
        assertEquals(CONFIRMED_AT, result.confirmedAt());
    }

    @Test
    @DisplayName("확정된 약속은 저장된 만료 시각이 지났어도 CONFIRMED다")
    void handle_confirmedPastExpiry_staysConfirmed() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.CONFIRMED, NOW.minus(Duration.ofHours(1)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertNull(result.expiresAt());
    }

    // ---- AC-8 다른 방 진행 중 제안 ----

    @Test
    @DisplayName("같은 글의 다른 교환에 만료 전 PROPOSED가 있으면 otherActiveProposalsOnPost가 true다")
    void handle_pendingProposalInOtherExchange_returnsTrue() {
        givenExchangeAndOtherExchange();
        appointmentRepository.preload(otherExchangeAppointment(AppointmentStatus.PROPOSED, NOW.plusSeconds(1)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertTrue(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("같은 글의 다른 교환에 약속이 없으면 otherActiveProposalsOnPost가 false다")
    void handle_noOtherProposal_returnsFalse() {
        givenExchangeAndOtherExchange();

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));

        assertFalse(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환의 PROPOSED 옛 만료 시각이 지금과 같아도 otherActiveProposalsOnPost가 true다")
    void handle_otherProposalAtOldExpiry_returnsTrue() {
        givenExchangeAndOtherExchange();
        appointmentRepository.preload(otherExchangeAppointment(AppointmentStatus.PROPOSED, NOW));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertTrue(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환의 PROPOSED가 제안 후 25시간 지났어도 otherActiveProposalsOnPost가 true다")
    void handle_otherProposalPastOldExpiry_returnsTrue() {
        givenExchangeAndOtherExchange();
        appointmentRepository.preload(otherExchangeAppointment(AppointmentStatus.PROPOSED,
                NOW.minus(Duration.ofHours(1))));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertTrue(result.otherActiveProposalsOnPost());
    }

    // ---- AC-9 지금 바로 약속 ----

    @Test
    @DisplayName("지금 바로 약속이면 NOW · 약속 시각 null · 내 알림 null이다")
    void handle_nowAppointment_returnsNullScheduledAtAndReminder() {
        givenExchange(ExchangeStatus.ACTIVE);
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), EXPIRES_AT, null, null, null, CREATED_AT, CREATED_AT));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentTimeType.NOW, result.timeType());
        assertNull(result.scheduledAt());
        assertNull(result.myReminderOffsetMinutes());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
    }

    // ---- AC-11 거절 · 철회 · 취소 ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "CANCELED"})
    @DisplayName("거절 · 철회 · 취소된 약속이면 저장 상태 그대로이고 만료 시각은 null이다")
    void handle_terminatedAppointment_returnsStoredStatus(AppointmentStatus status) {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(status, EXPIRES_AT);

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(status, result.status());
        assertNull(result.expiresAt());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "CANCELED"})
    @DisplayName("거절 · 철회 · 취소된 약속은 만료 시각이 지났어도 저장 상태 그대로다")
    void handle_terminatedPastExpiry_keepsStoredStatus(AppointmentStatus status) {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(status, NOW.minus(Duration.ofHours(1)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(status, result.status());
        assertNull(result.expiresAt());
    }

    // ---- AC-12 종료된 교환 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 종료됐어도 참여자는 약속을 조회한다")
    void handle_endedExchange_returnsResult(ExchangeStatus exchangeStatus) {
        givenExchange(exchangeStatus);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(EXCHANGE_ID, result.exchangeId());
    }

    // ---- helpers ----

    private void givenExchange(ExchangeStatus status) {
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0)),
                status, null, NOW, NOW));
    }

    private void givenAppointment(AppointmentStatus status, Instant expiresAt) {
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? CONFIRMED_AT : null;
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT));
    }

    private void givenExchangeAndOtherExchange() {
        givenExchange(ExchangeStatus.ACTIVE);
        givenAppointment(AppointmentStatus.PROPOSED, EXPIRES_AT);
        exchangeRepository.preload(Exchange.of(OTHER_EXCHANGE_ID, POST_ID, OTHER_REQUESTER_ID, OWNER_ID,
                OfferType.POST, List.of(OfferedItem.of(41L, 601L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
    }

    private static Appointment otherExchangeAppointment(AppointmentStatus status, Instant expiresAt) {
        return Appointment.of(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID, OTHER_REQUESTER_ID, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(41L)), expiresAt, 30, null, null, CREATED_AT, CREATED_AT);
    }
}
