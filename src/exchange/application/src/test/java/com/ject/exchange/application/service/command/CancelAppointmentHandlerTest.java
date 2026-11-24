package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.CancelAppointmentCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A-8 약속 취소 Handler 흐름 (#113). 저장 결과 · 판정 순서 · 거절 시 아무것도 바뀌지 않는지를 본다.
 */
class CancelAppointmentHandlerTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 15:30 (+09:00) 교환 확인 열림 시각 */
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    /** 2026-11-23 10:00 (+09:00) 임박 전 취소 시각 */
    private static final Instant NOW = Instant.parse("2026-11-23T01:00:00Z");
    /** 2026-11-22 17:00 (+09:00) */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long MISSING_APPOINTMENT_ID = 999L;
    private static final Long EXCHANGE_X = 1L;
    private static final Long POST_P = 10L;
    private static final Long POST_Q1 = 20L;
    private static final Long POST_Q2 = 21L;
    /** 제안자 A (교환 요청자) */
    private static final Long MEMBER_A = 101L;
    /** 받는 사람 B (글 작성자) */
    private static final Long MEMBER_B = 102L;
    /** 참여자가 아닌 C */
    private static final Long MEMBER_C = 103L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakePostPort postPort;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, CREATED_AT);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        postPort = new FakePostPort();

        postPort.register(post(POST_P, MEMBER_B, PostInfo.Status.RESERVED));
        postPort.register(post(POST_Q1, MEMBER_A, PostInfo.Status.RESERVED));
        postPort.register(post(POST_Q2, MEMBER_A, PostInfo.Status.AVAILABLE));
    }

    // ---- AC-1 제안자가 취소 ----

    @Test
    @DisplayName("제안자가 취소하면 약속 id를 반환하고 CANCELED · 취소한 사람 A · 결과 시각 now로 저장된다")
    void handle_byProposer_savesCanceled() {
        // given
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        // when
        Long appointmentId = handlerAt(NOW).handle(cancel(MEMBER_A));

        // then
        assertEquals(APPOINTMENT_ID, appointmentId);
        Appointment saved = stored();
        assertEquals(AppointmentStatus.CANCELED, saved.getStatus());
        assertEquals(MEMBER_A, saved.getCanceledByUserId());
        assertEquals(NOW, saved.getResolvedAt());
        assertEquals(Boolean.FALSE, saved.getCancelImminent());
        assertEquals(1, appointmentRepository.saveCount());
    }

    @Test
    @DisplayName("취소해도 교환은 저장하지 않고 ACTIVE 그대로다")
    void handle_cancel_keepsExchangeActive() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        handlerAt(NOW).handle(cancel(MEMBER_A));

        assertEquals(0, exchangeRepository.saveCount());
        assertEquals(ExchangeStatus.ACTIVE, exchangeRepository.findById(EXCHANGE_X).orElseThrow().getStatus());
    }

    // ---- AC-2 받은 사람이 취소 ----

    @Test
    @DisplayName("받는 사람이 취소하면 상대 동의 없이 CANCELED이고 취소한 사람은 B다")
    void handle_byReceiver_savesCanceled() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        Long appointmentId = handlerAt(NOW).handle(cancel(MEMBER_B));

        assertEquals(APPOINTMENT_ID, appointmentId);
        assertEquals(AppointmentStatus.CANCELED, stored().getStatus());
        assertEquals(MEMBER_B, stored().getCanceledByUserId());
    }

    // ---- AC-4 임박 여부 기록 ----

    @Test
    @DisplayName("서비스 시각 14:00:00에 취소하면 임박 true로 저장된다")
    void handle_oneHourBefore_savesImminent() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        handlerAt(SCHEDULED_AT.minus(Duration.ofHours(1))).handle(cancel(MEMBER_A));

        assertEquals(Boolean.TRUE, stored().getCancelImminent());
    }

    @Test
    @DisplayName("서비스 시각 13:59:59에 취소하면 임박 false로 저장된다")
    void handle_justBeforeImminent_savesNotImminent() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        handlerAt(SCHEDULED_AT.minus(Duration.ofHours(1)).minusSeconds(1)).handle(cancel(MEMBER_A));

        assertEquals(Boolean.FALSE, stored().getCancelImminent());
    }

    @Test
    @DisplayName("NOW 약속을 취소하면 임박 true로 저장된다")
    void handle_nowAppointment_savesImminent() {
        givenConfirmedNowAt(NOW);

        handlerAt(NOW.plus(Duration.ofMinutes(1))).handle(cancel(MEMBER_B));

        assertEquals(Boolean.TRUE, stored().getCancelImminent());
    }

    // ---- AC-5 참여자 아님 ----

    @Test
    @DisplayName("참여자가 아닌 C가 취소하면 EXCHANGE_002이고 약속 · 글 상태 그대로다")
    void handle_byThirdParty_throwsNotParticipant() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, () -> handlerAt(NOW).handle(cancel(MEMBER_C)));

        assertNothingChanged();
    }

    @Test
    @DisplayName("교환이 종료됐어도 참여자가 아니면 EXCHANGE_010보다 EXCHANGE_002가 먼저다")
    void handle_byThirdPartyOnClosedExchange_throwsNotParticipant() {
        givenConfirmed(scheduled(), ExchangeStatus.CLOSED);

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, () -> handlerAt(NOW).handle(cancel(MEMBER_C)));
    }

    // ---- AC-6 없는 약속 ----

    @Test
    @DisplayName("없는 약속 id로 취소하면 EXCHANGE_101이고 아무것도 저장하지 않는다")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        assertRejected(ExchangeErrorCode.APPOINTMENT_NOT_FOUND,
                () -> handlerAt(NOW).handle(new CancelAppointmentCommand(MISSING_APPOINTMENT_ID, MEMBER_A)));

        assertNothingChanged();
    }

    // ---- AC-7 확정이 아닌 약속 ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CONFIRMED")
    @DisplayName("CONFIRMED가 아닌 약속을 취소하면 EXCHANGE_109이고 상태 그대로 · release 호출 없음이다")
    void handle_notConfirmed_throwsInvalidStatus(AppointmentStatus status) {
        exchangeRepository.preload(exchangeX(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(inStatus(status));

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, () -> handlerAt(NOW).handle(cancel(MEMBER_A)));

        assertEquals(status, stored().getStatus());
        assertEquals(0, appointmentRepository.saveCount());
        assertTrue(postPort.releaseRequests().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NOT_MET", "CANCELED"})
    @DisplayName("교환이 끝난 뒤 남은 완료 · 못 만남 · 취소 약속을 취소하면 EXCHANGE_010이 아니라 EXCHANGE_109다")
    void handle_resolvedOnInactiveExchange_throwsInvalidStatus(AppointmentStatus status) {
        exchangeRepository.preload(exchangeX(ExchangeStatus.COMPLETED));
        appointmentRepository.preload(inStatus(status));

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, () -> handlerAt(NOW).handle(cancel(MEMBER_B)));

        assertTrue(postPort.releaseRequests().isEmpty());
    }

    // ---- AC-8 교환 확인이 열린 뒤 ----

    @Test
    @DisplayName("SCHEDULED 15:00 약속을 15:29:59에 취소하면 CANCELED로 저장된다")
    void handle_scheduledJustBeforeOpen_cancels() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        handlerAt(OPENS_AT.minusSeconds(1)).handle(cancel(MEMBER_A));

        assertEquals(AppointmentStatus.CANCELED, stored().getStatus());
    }

    @Test
    @DisplayName("SCHEDULED 15:00 약속을 15:30:00에 취소하면 EXCHANGE_207이고 상태 그대로 · release 호출 없음이다")
    void handle_scheduledAtOpen_throwsCancelAfterOpen() {
        givenConfirmed(scheduled(), ExchangeStatus.ACTIVE);

        assertRejected(ExchangeErrorCode.CANCEL_AFTER_CONFIRMATION_OPEN,
                () -> handlerAt(OPENS_AT).handle(cancel(MEMBER_A)));

        assertNothingChanged();
    }

    @Test
    @DisplayName("NOW 약속은 확정 10:00이면 10:29:59에 취소할 수 있다")
    void handle_nowJustBeforeOpen_cancels() {
        givenConfirmedNowAt(NOW);

        handlerAt(NOW.plus(Duration.ofMinutes(30)).minusSeconds(1)).handle(cancel(MEMBER_A));

        assertEquals(AppointmentStatus.CANCELED, stored().getStatus());
    }

    @Test
    @DisplayName("NOW 약속은 확정 10:00이면 10:30:00에 취소하면 EXCHANGE_207이다")
    void handle_nowAtOpen_throwsCancelAfterOpen() {
        givenConfirmedNowAt(NOW);

        assertRejected(ExchangeErrorCode.CANCEL_AFTER_CONFIRMATION_OPEN,
                () -> handlerAt(NOW.plus(Duration.ofMinutes(30))).handle(cancel(MEMBER_B)));

        assertNothingChanged();
    }

    // ---- AC-9 종료된 교환 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 ACTIVE가 아닌데 약속이 CONFIRMED로 남아 있으면 EXCHANGE_010이고 아무것도 바뀌지 않는다")
    void handle_inactiveExchange_throwsExchangeClosed(ExchangeStatus exchangeStatus) {
        givenConfirmed(scheduled(), exchangeStatus);

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED, () -> handlerAt(NOW).handle(cancel(MEMBER_A)));

        assertNothingChanged();
    }

    // ---- helpers ----

    private void assertNothingChanged() {
        assertEquals(AppointmentStatus.CONFIRMED, stored().getStatus());
        assertNull(stored().getCanceledByUserId());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(0, exchangeRepository.saveCount());
        assertTrue(postPort.releaseRequests().isEmpty());
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_Q1));
    }

    private static void assertRejected(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    private CancelAppointmentHandler handlerAt(Instant now) {
        return new CancelAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository, postPort,
                new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul"))));
    }

    private Appointment stored() {
        return appointmentRepository.findById(APPOINTMENT_ID).orElseThrow();
    }

    private void givenConfirmed(AppointmentTime time, ExchangeStatus exchangeStatus) {
        exchangeRepository.preload(exchangeX(exchangeStatus));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B,
                AppointmentStatus.CONFIRMED, time, PLACE, new AppointmentItemIds(List.of(ITEM_Q1)), EXPIRES_AT,
                30, 60, CONFIRMED_AT, null, List.of(), CREATED_AT, CREATED_AT));
    }

    private void givenConfirmedNowAt(Instant confirmedAt) {
        exchangeRepository.preload(exchangeX(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B,
                AppointmentStatus.CONFIRMED, new AppointmentTime(AppointmentTimeType.NOW, null), PLACE,
                new AppointmentItemIds(List.of(ITEM_Q1)), EXPIRES_AT, 30, 60, confirmedAt, null, List.of(),
                CREATED_AT, CREATED_AT));
    }

    private static Appointment inStatus(AppointmentStatus status) {
        boolean wasConfirmed = Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.NOT_MET,
                AppointmentStatus.CANCELED).contains(status);
        Instant confirmedAt = wasConfirmed ? CONFIRMED_AT : null;
        Instant resolvedAt = status == AppointmentStatus.PROPOSED ? null : NOW.minus(Duration.ofHours(1));
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B, status, scheduled(), PLACE,
                new AppointmentItemIds(List.of(ITEM_Q1)), EXPIRES_AT, 30, 60, confirmedAt, resolvedAt, List.of(),
                CREATED_AT, CREATED_AT);
    }

    private static CancelAppointmentCommand cancel(Long cancelerId) {
        return new CancelAppointmentCommand(APPOINTMENT_ID, cancelerId);
    }

    private static AppointmentTime scheduled() {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT);
    }

    /** 교환 X: A가 B의 글 P에 Q1 · Q2를 첨부 */
    private static Exchange exchangeX(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CREATED_AT : null;
        return Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0),
                        OfferedItem.of(ITEM_Q2, POST_Q2, null, null, null, 1)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    private static PostInfo post(Long id, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, "글 " + id, "post/" + id + "/1.jpg", ownerId, status);
    }
}
