package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
import com.ject.exchange.application.dto.command.RespondConfirmationCommand;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.ConfirmationSummary;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.service.query.GetAppointmentHandler;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.ConfirmationReply;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class RespondConfirmationHandlerTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 15:30 (+09:00) 교환 확인 열림 시각 = 공통 Given의 지금 */
    private static final Instant NOW = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    /** 이유 남기기 기한 = 열림 시각 + 3일 */
    private static final Instant REASON_DEADLINE = NOW.plus(Duration.ofDays(3));
    /** 2026-11-22 17:00 (+09:00) 확정 시각 */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long MISSING_APPOINTMENT_ID = 999L;
    private static final Long EXCHANGE_ID = 1L;
    /** 교환글 P0 */
    private static final Long POST_P0 = 10L;
    /** 요청자 A (제안자) */
    private static final Long REQUESTER_ID = 100L;
    /** 게시자 B (받는 사람) */
    private static final Long OWNER_ID = 200L;
    /** 참여자가 아닌 C */
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_P1 = 31L;
    private static final Long ITEM_P2 = 32L;
    /** A의 첨부 글 P1 (약속에서 고름) */
    private static final Long SOURCE_POST_P1 = 501L;
    /** A의 첨부 글 P2 (고르지 않음) */
    private static final Long SOURCE_POST_P2 = 502L;
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

        postPort.register(post(POST_P0, OWNER_ID));
        postPort.register(post(SOURCE_POST_P1, REQUESTER_ID));
        postPort.register(post(SOURCE_POST_P2, REQUESTER_ID));
    }

    // ---- AC-1 먼저 교환했어요 ----

    @Test
    @DisplayName("A가 DONE으로 답하면 약속 COMPLETED · 교환 COMPLETED · A의 응답이 저장되고 약속 id를 반환한다")
    void handle_doneByRequester_completesAppointmentAndExchange() {
        // given
        givenConfirmedAppointment();

        // when
        Long respondedId = handlerAt(NOW).handle(done(REQUESTER_ID));

        // then
        assertEquals(APPOINTMENT_ID, respondedId);
        Appointment saved = stored();
        assertEquals(AppointmentStatus.COMPLETED, saved.getStatus());
        assertEquals(NOW, saved.getResolvedAt());
        assertEquals(Optional.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, NOW)),
                saved.replyOf(REQUESTER_ID));
        assertTrue(saved.replyOf(OWNER_ID).isEmpty());
        Exchange exchange = storedExchange();
        assertEquals(ExchangeStatus.COMPLETED, exchange.getStatus());
        assertEquals(NOW, exchange.getCompletedAt());
        assertEquals(1, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("DONE이면 교환글 P0와 고른 첨부 글 P1의 교환 완료 요청이 한 번 가고 예약 해제 요청은 없다")
    void handle_done_requestsCompleteForPostAndChosenAttachedPost() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(done(REQUESTER_ID));

        assertEquals(1, postPort.completeRequests().size());
        assertEquals(Set.of(POST_P0, SOURCE_POST_P1), Set.copyOf(postPort.completeRequests().get(0)));
        assertTrue(postPort.releaseRequests().isEmpty());
        assertTrue(postPort.reserveRequests().isEmpty());
    }

    @Test
    @DisplayName("B가 먼저 DONE으로 답해도 COMPLETED이고 교환 완료 요청이 한 번 간다")
    void handle_doneByOwner_completes() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(done(OWNER_ID));

        assertEquals(AppointmentStatus.COMPLETED, stored().getStatus());
        assertEquals(ConfirmationAnswer.DONE, stored().replyOf(OWNER_ID).orElseThrow().answer());
        assertEquals(ExchangeStatus.COMPLETED, storedExchange().getStatus());
        assertEquals(1, postPort.completeRequests().size());
    }

    @Test
    @DisplayName("약속 전 알림을 둘 다 끈 약속도 DONE이면 COMPLETED다")
    void handle_reminderOff_stillCompletes() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.CONFIRMED, scheduled(), PLACE, new AppointmentItemIds(List.of(ITEM_P1)),
                EXPIRES_AT, null, null, CONFIRMED_AT, null, List.of(), CREATED_AT, CREATED_AT));

        handlerAt(NOW).handle(done(OWNER_ID));

        assertEquals(AppointmentStatus.COMPLETED, stored().getStatus());
    }

    @Test
    @DisplayName("새로 작성한 아이템 방식 교환이면 교환 완료 요청 대상은 P0뿐이다")
    void handle_newItemExchange_requestsCompleteForPostOnly() {
        // given
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_P0, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_P1, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(confirmedAppointment(scheduled()));

        // when
        handlerAt(NOW).handle(done(REQUESTER_ID));

        // then
        assertEquals(List.of(List.of(POST_P0)), postPort.completeRequests());
    }

    @Test
    @DisplayName("응답은 약속을 잠금 조회하기 전에 P0와 P1을 잠근다")
    void handle_locksInvolvedPostsBeforeLockedLookup() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(done(REQUESTER_ID));

        assertEquals(1, postLockRepository.lockedPostIds().size());
        assertEquals(Set.of(POST_P0, SOURCE_POST_P1), Set.copyOf(postLockRepository.lockedPostIds().get(0)));
        assertEquals(0, postLockRepository.lockedLookupCountsAtLock().get(0));
        assertTrue(appointmentRepository.lockedLookupCount() >= 1);
    }

    @Test
    @DisplayName("DONE 뒤 A-2로 보면 A는 myAnswer DONE · B는 myAnswer null이고 둘 다 결과 COMPLETED다")
    void handle_doneThenGetAppointment_showsResultPerViewer() {
        // given
        givenConfirmedAppointment();
        handlerAt(NOW).handle(done(REQUESTER_ID));
        GetAppointmentHandler getHandler = getHandlerAt(NOW.plusSeconds(60));

        // when
        AppointmentResult forRequester = getHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));
        AppointmentResult forOwner = getHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(AppointmentStatus.COMPLETED, forRequester.status());
        assertEquals(new ConfirmationSummary(NOW, AppointmentStatus.COMPLETED, false, ConfirmationAnswer.DONE, NOW),
                forRequester.confirmation());
        assertEquals(new ConfirmationSummary(NOW, AppointmentStatus.COMPLETED, false, null, NOW),
                forOwner.confirmation());
    }

    // ---- AC-2 먼저 못 만났어요 ----

    @Test
    @DisplayName("A가 NOT_MET으로 답하면 약속 NOT_MET · 교환 ACTIVE 그대로 · 이유가 저장된다")
    void handle_notMetByRequester_marksNotMetKeepsExchangeActive() {
        // given
        givenConfirmedAppointment();

        // when
        Long respondedId = handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        // then
        assertEquals(APPOINTMENT_ID, respondedId);
        Appointment saved = stored();
        assertEquals(AppointmentStatus.NOT_MET, saved.getStatus());
        assertEquals(NOW, saved.getResolvedAt());
        assertEquals(Optional.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.NO_SHOW, null, NOW)), saved.replyOf(REQUESTER_ID));
        assertEquals(ExchangeStatus.ACTIVE, storedExchange().getStatus());
        assertNull(storedExchange().getCompletedAt());
        assertEquals(0, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("NOT_MET이면 P0와 P1의 예약 해제 요청이 한 번 가고 교환 완료 요청은 없다")
    void handle_notMet_requestsReleaseForPostAndChosenAttachedPost() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        assertEquals(1, postPort.releaseRequests().size());
        assertEquals(Set.of(POST_P0, SOURCE_POST_P1), Set.copyOf(postPort.releaseRequests().get(0)));
        assertTrue(postPort.completeRequests().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(NotMetReason.class)
    @DisplayName("이유 6개 각각으로 NOT_MET이 저장된다")
    void handle_notMetEachReason_savesReason(NotMetReason reason) {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(notMet(REQUESTER_ID, reason, null));

        assertEquals(AppointmentStatus.NOT_MET, stored().getStatus());
        assertEquals(reason, stored().replyOf(REQUESTER_ID).orElseThrow().notMetReason());
    }

    @Test
    @DisplayName("OTHER와 직접 입력 100자는 보낸 값 그대로 저장된다")
    void handle_otherWithDetail_savesDetail() {
        givenConfirmedAppointment();
        String detail = "가".repeat(100);

        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.OTHER, detail));

        ConfirmationReply reply = stored().replyOf(REQUESTER_ID).orElseThrow();
        assertEquals(NotMetReason.OTHER, reply.notMetReason());
        assertEquals(detail, reply.notMetDetail());
    }

    // ---- AC-3 · AC-4 이유 남기기 ----

    @Test
    @DisplayName("NOT_MET 뒤 B가 이유를 남기면 NOT_MET · 결과 시각 그대로이고 교환 · post 요청은 바뀌지 않는다")
    void handle_reasonAfterNotMet_appendsReplyOnly() {
        // given
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));
        Instant later = NOW.plus(Duration.ofHours(5));

        // when
        Long respondedId = handlerAt(later).handle(notMet(OWNER_ID, NotMetReason.SELF_ABSENT, null));

        // then
        assertEquals(APPOINTMENT_ID, respondedId);
        Appointment saved = stored();
        assertEquals(AppointmentStatus.NOT_MET, saved.getStatus());
        assertEquals(NOW, saved.getResolvedAt());
        assertEquals(Optional.of(new ConfirmationReply(OWNER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.SELF_ABSENT, null, later)), saved.replyOf(OWNER_ID));
        assertEquals(NotMetReason.NO_SHOW, saved.replyOf(REQUESTER_ID).orElseThrow().notMetReason());
        assertEquals(ExchangeStatus.ACTIVE, storedExchange().getStatus());
        assertEquals(0, exchangeRepository.saveCount());
        assertEquals(1, postPort.releaseRequests().size());
        assertTrue(postPort.completeRequests().isEmpty());
    }

    @Test
    @DisplayName("이유를 남긴 B가 A-2로 보면 myAnswer NOT_MET · 결과 시각은 A가 답한 시각이다")
    void handle_reasonThenGetAppointment_showsOwnersAnswer() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));
        handlerAt(NOW.plusSeconds(600)).handle(notMet(OWNER_ID, NotMetReason.MUTUAL_CANCEL, null));

        AppointmentResult forOwner = getHandlerAt(NOW.plusSeconds(700))
                .handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.NOT_MET, forOwner.status());
        assertEquals(new ConfirmationSummary(NOW, AppointmentStatus.NOT_MET, false, ConfirmationAnswer.NOT_MET, NOW),
                forOwner.confirmation());
    }

    @Test
    @DisplayName("열림 시각 + 3일 1초 전이면 이유를 남길 수 있다")
    void handle_reasonJustBeforeDeadline_appends() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        handlerAt(REASON_DEADLINE.minusSeconds(1)).handle(notMet(OWNER_ID, NotMetReason.SELF_ABSENT, null));

        assertTrue(stored().replyOf(OWNER_ID).isPresent());
    }

    @Test
    @DisplayName("열림 시각 + 3일 정각에 이유를 남기면 EXCHANGE_303이고 저장값 그대로다")
    void handle_reasonAtDeadline_throwsAlreadyDecided() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED,
                () -> handlerAt(REASON_DEADLINE).handle(notMet(OWNER_ID, NotMetReason.SELF_ABSENT, null)));
        assertTrue(stored().replyOf(OWNER_ID).isEmpty());
    }

    // ---- AC-5 · AC-6 결과가 정해진 뒤 ----

    @Test
    @DisplayName("COMPLETED 뒤 B가 NOT_MET으로 답하면 EXCHANGE_303이고 B의 응답은 없다")
    void handle_notMetAfterCompleted_throwsAlreadyDecided() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(done(REQUESTER_ID));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(notMet(OWNER_ID, NotMetReason.NO_SHOW, null)));
        assertTrue(stored().replyOf(OWNER_ID).isEmpty());
        assertEquals(AppointmentStatus.COMPLETED, stored().getStatus());
    }

    @Test
    @DisplayName("COMPLETED 뒤 B가 DONE으로 답하면 교환이 COMPLETED여도 EXCHANGE_303이다")
    void handle_doneAfterCompleted_throwsAlreadyDecidedNotClosed() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(done(REQUESTER_ID));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(done(OWNER_ID)));
        assertTrue(stored().replyOf(OWNER_ID).isEmpty());
    }

    @Test
    @DisplayName("NOT_MET 뒤 B가 DONE으로 답하면 EXCHANGE_303이고 NOT_MET 그대로다")
    void handle_doneAfterNotMet_throwsAlreadyDecided() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(done(OWNER_ID)));
        assertEquals(AppointmentStatus.NOT_MET, stored().getStatus());
        assertEquals(ExchangeStatus.ACTIVE, storedExchange().getStatus());
        assertTrue(stored().replyOf(OWNER_ID).isEmpty());
    }

    // ---- AC-7 같은 사람이 다시 답함 ----

    @Test
    @DisplayName("DONE으로 답한 A가 다시 DONE · NOT_MET을 보내면 EXCHANGE_302")
    void handle_againAfterDone_throwsAlreadyAnswered() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(done(REQUESTER_ID));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(done(REQUESTER_ID)));
        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null)));
        assertEquals(ConfirmationAnswer.DONE, stored().replyOf(REQUESTER_ID).orElseThrow().answer());
    }

    @Test
    @DisplayName("NOT_MET으로 답한 A가 다시 보내면 EXCHANGE_302이고 처음 이유 그대로다")
    void handle_againAfterNotMet_throwsAlreadyAnswered() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(notMet(REQUESTER_ID, NotMetReason.OTHER, "다시")));
        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED,
                () -> handlerAt(NOW.plusSeconds(60)).handle(done(REQUESTER_ID)));
        assertEquals(NotMetReason.NO_SHOW, stored().replyOf(REQUESTER_ID).orElseThrow().notMetReason());
    }

    @Test
    @DisplayName("이유를 남긴 B가 다시 보내면 EXCHANGE_302")
    void handle_againAfterReason_throwsAlreadyAnswered() {
        givenConfirmedAppointment();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));
        handlerAt(NOW.plusSeconds(60)).handle(notMet(OWNER_ID, NotMetReason.SELF_ABSENT, null));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED,
                () -> handlerAt(NOW.plusSeconds(120)).handle(notMet(OWNER_ID, NotMetReason.ITEM_MISMATCH, null)));
        assertEquals(NotMetReason.SELF_ABSENT, stored().replyOf(OWNER_ID).orElseThrow().notMetReason());
    }

    // ---- AC-8 · AC-9 열림 시각 ----

    @Test
    @DisplayName("15:29:59에 답하면 EXCHANGE_301이고 저장값 그대로다")
    void handle_beforeOpen_throwsNotOpen() {
        givenConfirmedAppointment();

        assertRejected(ExchangeErrorCode.CONFIRMATION_NOT_OPEN,
                () -> handlerAt(NOW.minusSeconds(1)).handle(done(REQUESTER_ID)));
        assertEquals(AppointmentStatus.CONFIRMED, stored().getStatus());
        assertEquals(ExchangeStatus.ACTIVE, storedExchange().getStatus());
    }

    @Test
    @DisplayName("약속 시각 전에 NOT_MET으로 답해도 EXCHANGE_301")
    void handle_beforeScheduledAt_throwsNotOpen() {
        givenConfirmedAppointment();

        assertRejected(ExchangeErrorCode.CONFIRMATION_NOT_OPEN,
                () -> handlerAt(SCHEDULED_AT.minusSeconds(3600))
                        .handle(notMet(OWNER_ID, NotMetReason.NO_SHOW, null)));
    }

    @Test
    @DisplayName("14:00에 확정된 지금 바로 약속은 14:29에 EXCHANGE_301 · 14:30에 COMPLETED이고 askedAt은 14:30이다")
    void handle_nowAppointment_opensAtConfirmedPlus30() {
        // given
        Instant confirmedAt = Instant.parse("2026-11-23T05:00:00Z");
        Instant opensAt = confirmedAt.plus(Duration.ofMinutes(30));
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.CONFIRMED, new AppointmentTime(AppointmentTimeType.NOW, null), PLACE,
                new AppointmentItemIds(List.of(ITEM_P1)), EXPIRES_AT, null, null, confirmedAt, null, List.of(),
                CREATED_AT, CREATED_AT));

        // when · then
        assertRejected(ExchangeErrorCode.CONFIRMATION_NOT_OPEN,
                () -> handlerAt(opensAt.minusSeconds(60)).handle(done(REQUESTER_ID)));
        handlerAt(opensAt).handle(done(REQUESTER_ID));
        assertEquals(AppointmentStatus.COMPLETED, stored().getStatus());
        AppointmentResult result = getHandlerAt(opensAt).handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));
        assertEquals(opensAt, result.confirmation().askedAt());
    }

    // ---- AC-12 없는 약속 · 참여자 아님 ----

    @Test
    @DisplayName("없는 약속이면 EXCHANGE_101")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        givenConfirmedAppointment();

        assertRejected(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, () -> handlerAt(NOW).handle(
                new RespondConfirmationCommand(MISSING_APPOINTMENT_ID, REQUESTER_ID, ConfirmationAnswer.DONE,
                        null, null)));
    }

    @Test
    @DisplayName("A · B가 아닌 C가 답하면 EXCHANGE_002")
    void handle_thirdParty_throwsNotParticipant() {
        givenConfirmedAppointment();

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handlerAt(NOW).handle(done(OTHER_MEMBER_ID)));
    }

    @Test
    @DisplayName("C면 교환이 종료됐어도 EXCHANGE_002가 먼저다")
    void handle_thirdPartyOnClosedExchange_throwsNotParticipantFirst() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(confirmedAppointment(scheduled()));

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handlerAt(NOW).handle(done(OTHER_MEMBER_ID)));
    }

    // ---- AC-13 확정이 아닌 약속 ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
            names = {"PROPOSED", "REJECTED", "WITHDRAWN", "EXPIRED", "AUTO_REJECTED", "CANCELED"})
    @DisplayName("확정이 아닌 약속에 답하면 EXCHANGE_109이고 상태 그대로다")
    void handle_notConfirmed_throwsInvalidStatus(AppointmentStatus status) {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        Instant confirmedAt = status == AppointmentStatus.CANCELED ? CONFIRMED_AT : null;
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                scheduled(), PLACE, new AppointmentItemIds(List.of(ITEM_P1)), EXPIRES_AT, 30, null, confirmedAt,
                null, List.of(), CREATED_AT, CREATED_AT));

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> handlerAt(NOW).handle(done(OWNER_ID)));
        assertEquals(status, stored().getStatus());
    }

    @Test
    @DisplayName("만료 전 PROPOSED 약속에 답해도 EXCHANGE_109")
    void handle_pendingProposal_throwsInvalidStatus() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, scheduled(), PLACE, new AppointmentItemIds(List.of(ITEM_P1)),
                NOW.plus(Duration.ofDays(1)), 30, null, null, null, List.of(), CREATED_AT, CREATED_AT));

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS,
                () -> handlerAt(NOW).handle(notMet(OWNER_ID, NotMetReason.NO_SHOW, null)));
    }

    // ---- AC-14 종료된 교환 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"CLOSED", "LEFT"})
    @DisplayName("CONFIRMED 약속인데 교환이 종료 · 나감이면 EXCHANGE_010이고 저장값 그대로다")
    void handle_confirmedOnClosedExchange_throwsExchangeClosed(ExchangeStatus status) {
        exchangeRepository.preload(defaultExchange(status));
        appointmentRepository.preload(confirmedAppointment(scheduled()));

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED, () -> handlerAt(NOW).handle(done(REQUESTER_ID)));
        assertEquals(AppointmentStatus.CONFIRMED, stored().getStatus());
        assertEquals(status, storedExchange().getStatus());
    }

    @Test
    @DisplayName("교환이 종료됐으면 열림 시각 전이어도 EXCHANGE_010이 먼저다")
    void handle_closedExchangeBeforeOpen_throwsExchangeClosedFirst() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(confirmedAppointment(scheduled()));

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handlerAt(NOW.minusSeconds(60)).handle(done(REQUESTER_ID)));
    }

    @Test
    @DisplayName("NOT_MET 약속이면 교환이 종료됐어도 상대는 이유를 남길 수 있다")
    void handle_reasonOnClosedExchange_appends() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.NOT_MET, scheduled(), PLACE, new AppointmentItemIds(List.of(ITEM_P1)),
                EXPIRES_AT, 30, null, CONFIRMED_AT, NOW,
                List.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW,
                        null, NOW)), CREATED_AT, CREATED_AT));

        handlerAt(NOW.plusSeconds(60)).handle(notMet(OWNER_ID, NotMetReason.SELF_ABSENT, null));

        assertTrue(stored().replyOf(OWNER_ID).isPresent());
        assertTrue(postPort.releaseRequests().isEmpty());
    }

    // ---- AC-16 동시에 답함 (Fake에서는 순차 호출로 확인) ----

    @Test
    @DisplayName("DONE + DONE이면 먼저 처리된 쪽으로 COMPLETED이고 나중 쪽은 EXCHANGE_303 · 완료 요청은 1회다")
    void handle_doneAndDone_firstWins() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(done(OWNER_ID));
        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> handlerAt(NOW).handle(done(REQUESTER_ID)));

        assertEquals(AppointmentStatus.COMPLETED, stored().getStatus());
        assertTrue(stored().replyOf(REQUESTER_ID).isEmpty());
        assertEquals(1, postPort.completeRequests().size());
        assertEquals(1, exchangeRepository.saveCount());
        assertEquals(2, postLockRepository.lockedPostIds().size());
    }

    @Test
    @DisplayName("DONE + NOT_MET이면 COMPLETED이고 NOT_MET은 EXCHANGE_303 · 해제 요청은 없다")
    void handle_doneAndNotMet_firstWins() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(done(REQUESTER_ID));
        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED,
                () -> handlerAt(NOW).handle(notMet(OWNER_ID, NotMetReason.NO_SHOW, null)));

        assertEquals(AppointmentStatus.COMPLETED, stored().getStatus());
        assertEquals(1, postPort.completeRequests().size());
        assertTrue(postPort.releaseRequests().isEmpty());
    }

    @Test
    @DisplayName("NOT_MET + DONE이면 NOT_MET이고 DONE은 EXCHANGE_303 · 완료 요청은 없다")
    void handle_notMetAndDone_firstWins() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(notMet(OWNER_ID, NotMetReason.ITEM_MISMATCH, null));
        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> handlerAt(NOW).handle(done(REQUESTER_ID)));

        assertEquals(AppointmentStatus.NOT_MET, stored().getStatus());
        assertEquals(1, postPort.releaseRequests().size());
        assertTrue(postPort.completeRequests().isEmpty());
        assertEquals(ExchangeStatus.ACTIVE, storedExchange().getStatus());
    }

    @Test
    @DisplayName("NOT_MET + NOT_MET이면 나중 사람은 이유 남기기로 성공하고 해제 요청은 1회다")
    void handle_notMetAndNotMet_secondIsReason() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));
        handlerAt(NOW).handle(notMet(OWNER_ID, NotMetReason.NO_SHOW, null));

        assertEquals(AppointmentStatus.NOT_MET, stored().getStatus());
        assertTrue(stored().replyOf(REQUESTER_ID).isPresent());
        assertTrue(stored().replyOf(OWNER_ID).isPresent());
        assertEquals(1, postPort.releaseRequests().size());
    }

    @Test
    @DisplayName("같은 사람이 두 번 보내면 하나만 저장되고 다른 하나는 EXCHANGE_302다")
    void handle_samePersonTwice_savesOnce() {
        givenConfirmedAppointment();

        handlerAt(NOW).handle(done(REQUESTER_ID));
        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED, () -> handlerAt(NOW).handle(done(REQUESTER_ID)));

        assertEquals(1, appointmentRepository.saveCount());
        assertEquals(1, postPort.completeRequests().size());
    }

    // ---- AC-17 · AC-18 다른 방 수락 ----

    @Test
    @DisplayName("DONE 뒤 같은 글 P0의 다른 교환 E2 제안을 수락하면 EXCHANGE_005이고 PROPOSED 그대로다")
    void acceptAfterDone_samePostOtherExchange_throwsPostNotRequestable() {
        // given
        givenConfirmedAppointment();
        givenOtherProposals();
        handlerAt(NOW).handle(done(REQUESTER_ID));

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> acceptHandlerAt(NOW.plusSeconds(60)).handle(new AcceptAppointmentCommand(E2_APPOINTMENT_ID,
                        OWNER_ID, null)));

        // then
        assertEquals(ExchangeErrorCode.POST_NOT_REQUESTABLE, exception.getErrorCode());
        assertEquals(AppointmentStatus.PROPOSED, stored(E2_APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("DONE 뒤 P1을 고른 다른 교환 E3 제안을 수락하면 EXCHANGE_112이고 PROPOSED 그대로다")
    void acceptAfterDone_chosenAttachedPostOtherExchange_throwsItemNotAvailable() {
        givenConfirmedAppointment();
        givenOtherProposals();
        handlerAt(NOW).handle(done(REQUESTER_ID));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> acceptHandlerAt(NOW.plusSeconds(60)).handle(new AcceptAppointmentCommand(E3_APPOINTMENT_ID,
                        REQUESTER_ID, null)));

        assertEquals(ExchangeErrorCode.ITEM_NOT_AVAILABLE, exception.getErrorCode());
        assertEquals(AppointmentStatus.PROPOSED, stored(E3_APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("NOT_MET 뒤 같은 글 P0의 다른 교환 E2 제안을 수락하면 확정된다")
    void acceptAfterNotMet_samePostOtherExchange_confirms() {
        givenConfirmedAppointment();
        givenOtherProposals();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        Long acceptedId = acceptHandlerAt(NOW.plusSeconds(60)).handle(new AcceptAppointmentCommand(E2_APPOINTMENT_ID,
                OWNER_ID, null));

        assertEquals(E2_APPOINTMENT_ID, acceptedId);
        assertEquals(AppointmentStatus.CONFIRMED, stored(E2_APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("NOT_MET 뒤 P1을 고른 다른 교환 E3 제안을 수락하면 확정된다")
    void acceptAfterNotMet_chosenAttachedPostOtherExchange_confirms() {
        givenConfirmedAppointment();
        givenOtherProposals();
        handlerAt(NOW).handle(notMet(REQUESTER_ID, NotMetReason.NO_SHOW, null));

        Long acceptedId = acceptHandlerAt(NOW.plusSeconds(60)).handle(new AcceptAppointmentCommand(E3_APPOINTMENT_ID,
                REQUESTER_ID, null));

        assertEquals(E3_APPOINTMENT_ID, acceptedId);
        assertEquals(AppointmentStatus.CONFIRMED, stored(E3_APPOINTMENT_ID).getStatus());
    }

    // ---- helpers ----

    private static final Long E2_EXCHANGE_ID = 2L;
    private static final Long E2_APPOINTMENT_ID = 12L;
    private static final Long E2_ITEM = 41L;
    private static final Long E2_SOURCE_POST = 601L;
    private static final Long E3_EXCHANGE_ID = 3L;
    private static final Long E3_APPOINTMENT_ID = 13L;
    /** E3의 교환글 Q (게시자 D) */
    private static final Long E3_POST_Q = 20L;
    private static final Long E3_OWNER_D = 400L;
    private static final Long E3_ITEM_FROM_P1 = 51L;

    /**
     * E2: 같은 글 P0 · 요청자 C가 B에게 제안 (진행 중).
     * E3: D의 글 Q · A가 P1을 첨부해 고른 교환에서 D가 A에게 제안 (진행 중).
     */
    private void givenOtherProposals() {
        postPort.register(post(E2_SOURCE_POST, OTHER_MEMBER_ID));
        postPort.register(post(E3_POST_Q, E3_OWNER_D));
        exchangeRepository.preload(Exchange.of(E2_EXCHANGE_ID, POST_P0, OTHER_MEMBER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(E2_ITEM, E2_SOURCE_POST, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        exchangeRepository.preload(Exchange.of(E3_EXCHANGE_ID, E3_POST_Q, REQUESTER_ID, E3_OWNER_D, OfferType.POST,
                List.of(OfferedItem.of(E3_ITEM_FROM_P1, SOURCE_POST_P1, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        Instant otherExpiresAt = NOW.plus(Duration.ofDays(1));
        AppointmentTime otherTime = new AppointmentTime(AppointmentTimeType.SCHEDULED, NOW.plus(Duration.ofDays(2)));
        appointmentRepository.preload(Appointment.of(E2_APPOINTMENT_ID, E2_EXCHANGE_ID, OTHER_MEMBER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, otherTime, PLACE, new AppointmentItemIds(List.of(E2_ITEM)),
                otherExpiresAt, 30, null, null, null, List.of(), CREATED_AT, CREATED_AT));
        appointmentRepository.preload(Appointment.of(E3_APPOINTMENT_ID, E3_EXCHANGE_ID, E3_OWNER_D, REQUESTER_ID,
                AppointmentStatus.PROPOSED, otherTime, PLACE, new AppointmentItemIds(List.of(E3_ITEM_FROM_P1)),
                otherExpiresAt, 30, null, null, null, List.of(), CREATED_AT, CREATED_AT));
    }

    /** 거절되면 예외 코드가 맞고, 그 호출에서 약속 · 교환 저장과 post 요청이 늘지 않는다. */
    private void assertRejected(ExchangeErrorCode expected, Executable action) {
        int appointmentSavesBefore = appointmentRepository.saveCount();
        int exchangeSavesBefore = exchangeRepository.saveCount();
        int completeBefore = postPort.completeRequests().size();
        int releaseBefore = postPort.releaseRequests().size();
        Appointment before = appointmentRepository.findById(APPOINTMENT_ID).orElse(null);

        ExchangeException exception = assertThrows(ExchangeException.class, action);

        assertEquals(expected, exception.getErrorCode());
        assertEquals(appointmentSavesBefore, appointmentRepository.saveCount());
        assertEquals(exchangeSavesBefore, exchangeRepository.saveCount());
        assertEquals(completeBefore, postPort.completeRequests().size());
        assertEquals(releaseBefore, postPort.releaseRequests().size());
        if (before != null) {
            assertEquals(before.getStatus(), stored().getStatus());
            assertEquals(before.getResolvedAt(), stored().getResolvedAt());
        }
    }

    private RespondConfirmationHandler handlerAt(Instant now) {
        return new RespondConfirmationHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, serviceTime(now));
    }

    private GetAppointmentHandler getHandlerAt(Instant now) {
        return new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort, serviceTime(now));
    }

    private AcceptAppointmentHandler acceptHandlerAt(Instant now) {
        return new AcceptAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, serviceTime(now));
    }

    private static ServiceTime serviceTime(Instant now) {
        return new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul")));
    }

    private void givenConfirmedAppointment() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(confirmedAppointment(scheduled()));
    }

    private Appointment stored() {
        return stored(APPOINTMENT_ID);
    }

    private Appointment stored(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    private Exchange storedExchange() {
        return exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
    }

    private static RespondConfirmationCommand done(Long memberId) {
        return new RespondConfirmationCommand(APPOINTMENT_ID, memberId, ConfirmationAnswer.DONE, null, null);
    }

    private static RespondConfirmationCommand notMet(Long memberId, NotMetReason reason, String detail) {
        return new RespondConfirmationCommand(APPOINTMENT_ID, memberId, ConfirmationAnswer.NOT_MET, reason, detail);
    }

    /** 교환 E: 교환글 P0 · 첨부 글 P1(ITEM_P1) · P2(ITEM_P2) */
    private static Exchange defaultExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CREATED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_P0, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_P1, SOURCE_POST_P1, null, null, null, 0),
                        OfferedItem.of(ITEM_P2, SOURCE_POST_P2, null, null, null, 1)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    /** A가 제안하고 B가 수락한 약속 M (P1 선택 · 응답 없음) */
    private static Appointment confirmedAppointment(AppointmentTime time) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED,
                time, PLACE, new AppointmentItemIds(List.of(ITEM_P1)), EXPIRES_AT, 30, 60, CONFIRMED_AT,
                null, List.of(), CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduled() {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT);
    }

    private static PostInfo post(Long id, Long authorId) {
        return new PostInfo(id, "글 " + id, "https://cdn.example.com/post/" + id + "/1.jpg", authorId,
                PostInfo.Status.AVAILABLE);
    }
}
