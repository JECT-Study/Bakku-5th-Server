package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.UpdateReminderCommand;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.service.query.GetAppointmentHandler;
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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class UpdateReminderHandlerTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00), 내일 15:00 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-27 14:00 (+09:00) */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = NOW.minus(Duration.ofHours(2));
    private static final Instant CONFIRMED_AT = NOW.minus(Duration.ofHours(1));
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long MISSING_APPOINTMENT_ID = 999L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    /** 제안자 A (교환 요청자) */
    private static final Long REQUESTER_ID = 100L;
    /** 받는 사람 B (글 게시자) */
    private static final Long OWNER_ID = 200L;
    /** 참여자가 아닌 C */
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    private static final Long SOURCE_POST_31 = 501L;
    private static final Integer PROPOSER_REMINDER = 30;
    private static final Integer RECEIVER_REMINDER = 60;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private ServiceTime serviceTime;
    private UpdateReminderHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postPort = new FakePostPort();
        serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new UpdateReminderHandler(appointmentRepository, exchangeRepository, serviceTime);

        postPort.register(post(POST_ID, OWNER_ID));
        postPort.register(post(SOURCE_POST_31, REQUESTER_ID));
    }

    // ---- 제안자 변경 (AC-1) ----

    @Test
    @DisplayName("제안자가 알림을 10으로 바꾸면 제안자 알림 10으로 저장되고 받는 사람 알림 · 상태 · 시간 · 장소 · 아이템 · 확정 시각은 그대로다")
    void handle_byProposer_savesProposerReminderOnly() {
        // given
        givenDefaultExchangeAndConfirmed();

        // when
        Long updatedId = handler.handle(command(REQUESTER_ID, 10));

        // then
        assertEquals(APPOINTMENT_ID, updatedId);
        assertEquals(1, appointmentRepository.saveCount());
        Appointment saved = stored();
        assertEquals(10, saved.getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, saved.getReceiverReminderOffsetMinutes());
        assertEquals(AppointmentStatus.CONFIRMED, saved.getStatus());
        assertEquals(EXCHANGE_ID, saved.getExchangeId());
        assertEquals(REQUESTER_ID, saved.getProposerUserId());
        assertEquals(OWNER_ID, saved.getReceiverUserId());
        assertEquals(AppointmentTimeType.SCHEDULED, saved.getTime().type());
        assertEquals(SCHEDULED_AT, saved.getTime().scheduledAt());
        assertEquals(PLACE, saved.getPlace());
        assertEquals(List.of(ITEM_31), saved.getItemIds().values());
        assertEquals(CONFIRMED_AT, saved.getConfirmedAt());
        assertEquals(0, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("변경 후 제안자가 조회하면 CONFIRMED · 내 알림 10이다")
    void handle_thenGetAppointment_returnsProposerReminder() {
        // given
        givenDefaultExchangeAndConfirmed();
        GetAppointmentHandler getHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        // when
        Long updatedId = handler.handle(command(REQUESTER_ID, 10));
        AppointmentResult result = getHandler.handle(new GetAppointmentQuery(updatedId, REQUESTER_ID));

        // then
        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertEquals(10, result.myReminderOffsetMinutes());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 1440})
    @DisplayName("제안자 알림 경계값 10 · 1440이면 그 값으로 저장된다")
    void handle_boundaryValue_saves(int reminder) {
        givenDefaultExchangeAndConfirmed();

        handler.handle(command(REQUESTER_ID, reminder));

        assertEquals(reminder, stored().getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, stored().getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("변경은 약속을 잠금 조회한다")
    void handle_usesLockedLookup() {
        givenDefaultExchangeAndConfirmed();

        handler.handle(command(REQUESTER_ID, 10));

        assertTrue(appointmentRepository.lockedLookupCount() >= 1);
    }

    // ---- 받는 사람 변경 (AC-2) ----

    @Test
    @DisplayName("받는 사람이 알림을 1440으로 바꾸면 받는 사람 알림 1440으로 저장되고 제안자 알림은 30 그대로다")
    void handle_byReceiver_savesReceiverReminderOnly() {
        givenDefaultExchangeAndConfirmed();

        Long updatedId = handler.handle(command(OWNER_ID, 1440));

        assertEquals(APPOINTMENT_ID, updatedId);
        Appointment saved = stored();
        assertEquals(1440, saved.getReceiverReminderOffsetMinutes());
        assertEquals(PROPOSER_REMINDER, saved.getProposerReminderOffsetMinutes());
        assertEquals(AppointmentStatus.CONFIRMED, saved.getStatus());
    }

    @Test
    @DisplayName("변경 후 받는 사람이 조회하면 내 알림 1440 · 제안자가 조회하면 내 알림 30이다")
    void handle_byReceiver_thenGetAppointment_returnsPerViewerReminder() {
        givenDefaultExchangeAndConfirmed();
        GetAppointmentHandler getHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        handler.handle(command(OWNER_ID, 1440));

        assertEquals(1440, getHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID))
                .myReminderOffsetMinutes());
        assertEquals(PROPOSER_REMINDER, getHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID))
                .myReminderOffsetMinutes());
    }

    // ---- 끄기 (AC-3) ----

    @Test
    @DisplayName("제안자가 알림을 null로 바꾸면 제안자 알림 null로 저장되고 받는 사람 알림은 60 그대로다")
    void handle_nullReminder_turnsOffProposerReminder() {
        givenDefaultExchangeAndConfirmed();

        handler.handle(command(REQUESTER_ID, null));

        assertNull(stored().getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, stored().getReceiverReminderOffsetMinutes());
        assertEquals(AppointmentStatus.CONFIRMED, stored().getStatus());
    }

    // ---- 알림 시각 경과 (AC-4) ----

    @Test
    @DisplayName("약속 20분 전에 알림 30으로 바꾸면 알림 시각이 지났어도 30으로 저장된다")
    void handle_reminderTimeAlreadyPassed_saves() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmed(AppointmentStatus.CONFIRMED,
                scheduledTime(NOW.plus(Duration.ofMinutes(20))), PROPOSER_REMINDER, RECEIVER_REMINDER));

        handler.handle(command(REQUESTER_ID, 30));

        assertEquals(1, appointmentRepository.saveCount());
        assertEquals(30, stored().getProposerReminderOffsetMinutes());
        assertEquals(RECEIVER_REMINDER, stored().getReceiverReminderOffsetMinutes());
    }

    // ---- 없는 약속 (AC-7) ----

    @Test
    @DisplayName("없는 약속이면 EXCHANGE_101")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        givenDefaultExchangeAndConfirmed();

        assertUpdateFails(ExchangeErrorCode.APPOINTMENT_NOT_FOUND,
                () -> handler.handle(new UpdateReminderCommand(MISSING_APPOINTMENT_ID, REQUESTER_ID, 10)));
    }

    // ---- 참여자 아님 (AC-8) ----

    @Test
    @DisplayName("참여자가 아닌 C가 바꾸면 EXCHANGE_002이고 저장값은 그대로다")
    void handle_thirdParty_throwsNotParticipant() {
        givenDefaultExchangeAndConfirmed();

        assertUpdateFails(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(OTHER_MEMBER_ID, 10)));
    }

    @Test
    @DisplayName("참여자가 아니면 교환이 종료됐어도 EXCHANGE_002가 먼저다")
    void handle_thirdPartyOnClosedExchange_throwsNotParticipant() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(defaultConfirmed());

        assertUpdateFails(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(OTHER_MEMBER_ID, 10)));
    }

    @Test
    @DisplayName("참여자가 아니면 약속이 확정이 아니어도 EXCHANGE_002가 먼저다")
    void handle_thirdPartyOnCanceledAppointment_throwsNotParticipant() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmed(AppointmentStatus.CANCELED, scheduledTime(SCHEDULED_AT),
                PROPOSER_REMINDER, RECEIVER_REMINDER));

        assertUpdateFails(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(OTHER_MEMBER_ID, 10)));
    }

    // ---- 확정이 아닌 약속 (AC-9) ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"PROPOSED", "REJECTED", "WITHDRAWN", "EXPIRED",
            "AUTO_REJECTED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("약속이 CONFIRMED가 아니면 EXCHANGE_109이고 상태와 알림은 그대로다")
    void handle_notConfirmed_throwsInvalidAppointmentStatus(AppointmentStatus status) {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmed(status, scheduledTime(SCHEDULED_AT), PROPOSER_REMINDER,
                RECEIVER_REMINDER));

        assertUpdateFails(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> handler.handle(command(REQUESTER_ID, 10)));
        assertEquals(status, stored().getStatus());
    }

    @Test
    @DisplayName("만료된 PROPOSED 제안이면 EXCHANGE_109")
    void handle_expiredProposal_throwsInvalidAppointmentStatus() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED, scheduledTime(SCHEDULED_AT),
                NOW.minus(Duration.ofMinutes(1)), PROPOSER_REMINDER, null, null));

        assertUpdateFails(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> handler.handle(command(REQUESTER_ID, 10)));
    }

    @Test
    @DisplayName("확정이 아닌 지금 바로 약속이면 EXCHANGE_110보다 EXCHANGE_109가 먼저다")
    void handle_proposedNowAppointment_throwsInvalidAppointmentStatus() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED, nowTime(),
                NOW.plus(Duration.ofHours(23)), null, null, null));

        assertUpdateFails(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> handler.handle(command(REQUESTER_ID, 30)));
    }

    @Test
    @DisplayName("확정이 아니고 약속 시각도 지났으면 EXCHANGE_107보다 EXCHANGE_109가 먼저다")
    void handle_completedAfterScheduledAt_throwsInvalidAppointmentStatus() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmed(AppointmentStatus.COMPLETED,
                scheduledTime(NOW.minus(Duration.ofHours(1))), PROPOSER_REMINDER, RECEIVER_REMINDER));

        assertUpdateFails(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> handler.handle(command(REQUESTER_ID, 10)));
    }

    // ---- 약속 시각 지남 (AC-10) ----

    @Test
    @DisplayName("확정 약속의 약속 시각이 지금과 같으면 EXCHANGE_107이고 저장값은 그대로다")
    void handle_scheduledAtNow_throwsScheduledAtPassed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmed(AppointmentStatus.CONFIRMED, scheduledTime(NOW),
                PROPOSER_REMINDER, RECEIVER_REMINDER));

        assertUpdateFails(ExchangeErrorCode.SCHEDULED_AT_PASSED, () -> handler.handle(command(REQUESTER_ID, 10)));
    }

    @Test
    @DisplayName("확정 약속의 약속 시각이 지났으면 EXCHANGE_107")
    void handle_scheduledAtPassed_throwsScheduledAtPassed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmed(AppointmentStatus.CONFIRMED,
                scheduledTime(NOW.minus(Duration.ofHours(1))), PROPOSER_REMINDER, RECEIVER_REMINDER));

        assertUpdateFails(ExchangeErrorCode.SCHEDULED_AT_PASSED, () -> handler.handle(command(OWNER_ID, null)));
    }

    // ---- 지금 바로 약속 (AC-11) ----

    @Test
    @DisplayName("지금 바로 확정 약속에 알림 30을 주면 EXCHANGE_110이고 저장값 null 그대로다")
    void handle_nowAppointmentWithValue_throwsReminderNotAllowed() {
        givenNowConfirmed();

        assertUpdateFails(ExchangeErrorCode.REMINDER_NOT_ALLOWED, () -> handler.handle(command(REQUESTER_ID, 30)));
        assertNull(stored().getProposerReminderOffsetMinutes());
        assertNull(stored().getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("지금 바로 확정 약속에 알림 null을 줘도 EXCHANGE_110")
    void handle_nowAppointmentWithNull_throwsReminderNotAllowed() {
        givenNowConfirmed();

        assertUpdateFails(ExchangeErrorCode.REMINDER_NOT_ALLOWED, () -> handler.handle(command(OWNER_ID, null)));
    }

    // ---- 종료된 교환 (AC-12) ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 진행 중이 아니면 EXCHANGE_010이고 저장값은 그대로다")
    void handle_exchangeNotActive_throwsExchangeClosed(ExchangeStatus status) {
        exchangeRepository.preload(defaultExchange(status));
        appointmentRepository.preload(defaultConfirmed());

        assertUpdateFails(ExchangeErrorCode.EXCHANGE_CLOSED, () -> handler.handle(command(REQUESTER_ID, 10)));
    }

    @Test
    @DisplayName("교환이 종료됐고 약속도 확정이 아니면 EXCHANGE_010이 먼저다")
    void handle_closedExchangeAndCanceledAppointment_throwsExchangeClosed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.LEFT));
        appointmentRepository.preload(confirmed(AppointmentStatus.CANCELED, scheduledTime(SCHEDULED_AT),
                PROPOSER_REMINDER, RECEIVER_REMINDER));

        assertUpdateFails(ExchangeErrorCode.EXCHANGE_CLOSED, () -> handler.handle(command(REQUESTER_ID, 10)));
    }

    // ---- helpers ----

    /** 변경이 막히면 예외 코드가 맞고, 어떤 약속 · 교환도 저장되지 않으며 두 알림은 그대로다. */
    private void assertUpdateFails(ExchangeErrorCode expected, Executable action) {
        Appointment before = appointmentRepository.findById(APPOINTMENT_ID).orElse(null);
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(0, exchangeRepository.saveCount());
        if (before != null) {
            Appointment after = stored();
            assertEquals(before.getStatus(), after.getStatus());
            assertEquals(before.getProposerReminderOffsetMinutes(), after.getProposerReminderOffsetMinutes());
            assertEquals(before.getReceiverReminderOffsetMinutes(), after.getReceiverReminderOffsetMinutes());
        }
    }

    private void givenDefaultExchangeAndConfirmed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(defaultConfirmed());
    }

    private void givenNowConfirmed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, nowTime(),
                NOW.plus(Duration.ofHours(23)), null, null, CONFIRMED_AT));
    }

    private Appointment stored() {
        return appointmentRepository.findById(APPOINTMENT_ID).orElseThrow();
    }

    private static UpdateReminderCommand command(Long memberId, Integer reminderOffsetMinutes) {
        return new UpdateReminderCommand(APPOINTMENT_ID, memberId, reminderOffsetMinutes);
    }

    /** 교환 E: 요청자 A · 게시자 B · 글 G · 첨부 글 S1(ITEM_31) */
    private static Exchange defaultExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CREATED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    /** A가 제안하고 B가 수락한 내일 15:00 약속 P (A 알림 30 · B 알림 60) */
    private static Appointment defaultConfirmed() {
        return confirmed(AppointmentStatus.CONFIRMED, scheduledTime(SCHEDULED_AT), PROPOSER_REMINDER,
                RECEIVER_REMINDER);
    }

    private static Appointment confirmed(AppointmentStatus status, AppointmentTime time, Integer proposerReminder,
                                         Integer receiverReminder) {
        return appointment(status, time, EXPIRES_AT, proposerReminder, receiverReminder, CONFIRMED_AT);
    }

    private static Appointment appointment(AppointmentStatus status, AppointmentTime time, Instant expiresAt,
                                           Integer proposerReminder, Integer receiverReminder,
                                           Instant confirmedAt) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status, time, PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), expiresAt, proposerReminder, receiverReminder,
                confirmedAt, CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduledTime(Instant scheduledAt) {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt);
    }

    private static AppointmentTime nowTime() {
        return new AppointmentTime(AppointmentTimeType.NOW, null);
    }

    private static PostInfo post(Long id, Long authorId) {
        return new PostInfo(id, "글 " + id, "https://cdn.example.com/post/" + id + "/1.jpg", authorId,
                PostInfo.Status.AVAILABLE);
    }
}
