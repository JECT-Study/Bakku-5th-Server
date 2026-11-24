package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AcceptAppointmentHandlerTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-27 14:00 (+09:00) */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = NOW.minus(Duration.ofHours(1));
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long MISSING_APPOINTMENT_ID = 999L;
    private static final Long EXCHANGE_ID = 1L;
    /** 글 G */
    private static final Long POST_ID = 10L;
    /** 다른 글 H */
    private static final Long OTHER_POST_ID = 11L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    private static final Long ITEM_32 = 32L;
    /** 첨부 글 S1 (ITEM_31의 원본) */
    private static final Long SOURCE_POST_31 = 501L;
    /** 첨부 글 S2 (ITEM_32의 원본) */
    private static final Long SOURCE_POST_32 = 502L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakePostPort postPort;
    private ServiceTime serviceTime;
    private AcceptAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        postPort = new FakePostPort();
        serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new AcceptAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, serviceTime);

        postPort.register(post(POST_ID, OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(post(OTHER_POST_ID, OTHER_MEMBER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(post(SOURCE_POST_31, REQUESTER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(post(SOURCE_POST_32, REQUESTER_ID, PostInfo.Status.AVAILABLE));
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("받는 사람이 수락하면 CONFIRMED · 확정 시각 · 받는 사람 알림 30이 저장되고 시간 · 장소 · 아이템 · 제안자 알림은 그대로다")
    void handle_byReceiver_confirmsAppointment() {
        // given
        givenDefaultExchangeAndProposal();

        // when
        Long acceptedId = handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(APPOINTMENT_ID, acceptedId);
        Appointment saved = stored(APPOINTMENT_ID);
        assertEquals(AppointmentStatus.CONFIRMED, saved.getStatus());
        assertEquals(NOW, saved.getConfirmedAt());
        assertEquals(EXCHANGE_ID, saved.getExchangeId());
        assertEquals(REQUESTER_ID, saved.getProposerUserId());
        assertEquals(OWNER_ID, saved.getReceiverUserId());
        assertEquals(AppointmentTimeType.SCHEDULED, saved.getTime().type());
        assertEquals(SCHEDULED_AT, saved.getTime().scheduledAt());
        assertEquals(PLACE, saved.getPlace());
        assertEquals(List.of(ITEM_31), saved.getItemIds().values());
        assertEquals(30, saved.getProposerReminderOffsetMinutes());
        assertEquals(30, saved.getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("수락 후 받는 사람이 조회하면 CONFIRMED · 확정 시각 · 만료 null · 내 알림 30이다")
    void handle_thenGetAppointment_returnsConfirmedForAccepter() {
        // given
        givenDefaultExchangeAndProposal();
        GetAppointmentHandler getHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        // when
        Long acceptedId = handler.handle(command(APPOINTMENT_ID, OWNER_ID));
        AppointmentResult result = getHandler.handle(new GetAppointmentQuery(acceptedId, OWNER_ID));

        // then
        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertEquals(NOW, result.confirmedAt());
        assertNull(result.expiresAt());
        assertEquals(30, result.myReminderOffsetMinutes());
    }

    @Test
    @DisplayName("약속 시각이 20분 뒤라 알림 시각이 이미 지났어도 받는 사람 알림 30이 저장된다")
    void handle_reminderAlreadyPassed_savesThirty() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(null, NOW.plus(Duration.ofMinutes(20))));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(30, stored(APPOINTMENT_ID).getReceiverReminderOffsetMinutes());
    }

    @Test
    @DisplayName("지금 바로 약속을 수락하면 CONFIRMED이고 두 사람 알림 null이며 조회 응답 내 알림도 null이다")
    void handle_nowAppointment_noReminder() {
        // given
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), null, null, null, null, CREATED_AT, CREATED_AT));
        GetAppointmentHandler getHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        // when
        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        Appointment saved = stored(APPOINTMENT_ID);
        assertEquals(AppointmentStatus.CONFIRMED, saved.getStatus());
        assertNull(saved.getReceiverReminderOffsetMinutes());
        assertNull(saved.getProposerReminderOffsetMinutes());
        assertNull(getHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID)).myReminderOffsetMinutes());
        assertNull(getHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID)).myReminderOffsetMinutes());
    }

    @Test
    @DisplayName("만료 시각 1초 전에 수락하면 확정된다")
    void handle_justBeforeExpiry_confirms() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW.plusSeconds(1), NOW.plusSeconds(1)));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 자동 거절 (AC-6) ----

    @Test
    @DisplayName("같은 글의 다른 교환의 PROPOSED 제안은 옛 만료 시각과 상관없이 AUTO_REJECTED · 다른 글 제안은 그대로다")
    void handle_otherProposalsOnSamePost_autoRejectsAllPending() {
        // given
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, 601L, 0)));
        preloadOtherExchange(3L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(42L, 602L, 0)));
        preloadOtherExchange(4L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(43L, 603L, 0)));
        preloadOtherExchange(5L, OTHER_POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(44L, 604L, 0)));
        preloadOtherExchange(8L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(48L, 608L, 0)));
        appointmentRepository.preload(otherProposal(12L, 2L, EXPIRES_AT, List.of(41L)));
        appointmentRepository.preload(otherProposal(13L, 3L, null, List.of(42L)));
        appointmentRepository.preload(otherProposal(14L, 4L, NOW.minus(Duration.ofMinutes(1)), List.of(43L)));
        appointmentRepository.preload(otherProposal(15L, 5L, EXPIRES_AT, List.of(44L)));
        appointmentRepository.preload(otherProposal(18L, 8L, NOW, List.of(48L)));

        // when
        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(12L).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(13L).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(14L).getStatus());
        assertEquals(AppointmentStatus.PROPOSED, stored(15L).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(18L).getStatus());
        assertEquals(ExchangeStatus.ACTIVE, exchangeRepository.findById(2L).orElseThrow().getStatus());
        assertEquals(ExchangeStatus.ACTIVE, exchangeRepository.findById(3L).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("교환 X 제안을 수락하면 같은 글 교환 Y의 제안 후 25시간 지난 PROPOSED 제안이 AUTO_REJECTED다")
    void handle_staleProposalInOtherRoom_autoRejected() {
        // given
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, 601L, 0)));
        Instant proposedAt = NOW.minus(Duration.ofHours(25));
        appointmentRepository.preload(Appointment.of(12L, 2L, OTHER_MEMBER_ID + 2L, OWNER_ID + 2L,
                AppointmentStatus.PROPOSED, scheduledTime(SCHEDULED_AT), PLACE, new AppointmentItemIds(List.of(41L)),
                proposedAt.plus(Duration.ofHours(24)), 30, null, null, proposedAt, proposedAt));

        // when
        Long acceptedId = handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(APPOINTMENT_ID, acceptedId);
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(12L).getStatus());
    }

    @Test
    @DisplayName("자동 거절된 제안은 상태만 바뀌고 시간 · 아이템 · 제안자는 그대로다")
    void handle_autoRejected_keepsOtherFields() {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, 601L, 0)));
        Appointment q2 = otherProposal(12L, 2L, EXPIRES_AT, List.of(41L));
        appointmentRepository.preload(q2);

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        Appointment rejected = stored(12L);
        assertEquals(AppointmentStatus.AUTO_REJECTED, rejected.getStatus());
        assertEquals(q2.getExchangeId(), rejected.getExchangeId());
        assertEquals(q2.getProposerUserId(), rejected.getProposerUserId());
        assertEquals(q2.getTime(), rejected.getTime());
        assertEquals(q2.getItemIds(), rejected.getItemIds());
        assertEquals(q2.getExpiresAt(), rejected.getExpiresAt());
        assertNull(rejected.getConfirmedAt());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"LEFT", "CLOSED"})
    @DisplayName("같은 글의 다른 교환이 나감 · 종료 상태여도 그 교환의 만료 전 제안은 AUTO_REJECTED다")
    void handle_otherExchangeNotActive_stillAutoRejects(ExchangeStatus otherStatus) {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, otherStatus, List.of(postItem(41L, 601L, 0)));
        appointmentRepository.preload(otherProposal(12L, 2L, EXPIRES_AT, List.of(41L)));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(12L).getStatus());
        assertEquals(otherStatus, exchangeRepository.findById(2L).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("고른 첨부 글 S1 또는 글 G에 걸린 다른 교환의 제안은 AUTO_REJECTED · S1을 고르지 않은 제안은 그대로다")
    void handle_proposalsInvolvingChosenAttachedPost_autoRejected() {
        // given
        givenDefaultExchangeAndProposal();
        // E6: S1이 본 글
        preloadOtherExchange(6L, SOURCE_POST_31, ExchangeStatus.ACTIVE, List.of(postItem(61L, 606L, 0)));
        // E7: 다른 글 · S1을 첨부하고 그 제안이 S1을 고름
        preloadOtherExchange(7L, OTHER_POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(71L, SOURCE_POST_31, 0)));
        // E9: 다른 글 · G를 첨부하고 그 제안이 G를 고름
        preloadOtherExchange(9L, 12L, ExchangeStatus.ACTIVE, List.of(postItem(91L, POST_ID, 0)));
        // E10: 다른 글 · S1을 첨부했지만 그 제안은 다른 아이템을 고름
        preloadOtherExchange(10L, 13L, ExchangeStatus.ACTIVE,
                List.of(postItem(101L, SOURCE_POST_31, 0), postItem(102L, 610L, 1)));
        appointmentRepository.preload(otherProposal(16L, 6L, EXPIRES_AT, List.of(61L)));
        appointmentRepository.preload(otherProposal(17L, 7L, EXPIRES_AT, List.of(71L)));
        appointmentRepository.preload(otherProposal(19L, 9L, EXPIRES_AT, List.of(91L)));
        appointmentRepository.preload(otherProposal(20L, 10L, EXPIRES_AT, List.of(102L)));

        // when
        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(16L).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(17L).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(19L).getStatus());
        assertEquals(AppointmentStatus.PROPOSED, stored(20L).getStatus());
    }

    // ---- 예약중 요청 (AC-7) ----

    @Test
    @DisplayName("수락하면 예약중 요청이 한 번 가고 대상은 글 G와 고른 첨부 글 S1이다")
    void handle_reservesPostAndChosenAttachedPost() {
        givenDefaultExchangeAndProposal();

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(List.of(List.of(POST_ID, SOURCE_POST_31)), postPort.reserveRequests());
    }

    @Test
    @DisplayName("새로 작성한 아이템만 고른 약속을 수락하면 예약중 요청 대상은 글 G뿐이다")
    void handle_newItemOnly_reservesPostOnly() {
        // given
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_31, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        // when
        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(List.of(List.of(POST_ID)), postPort.reserveRequests());
    }

    // ---- 잠금 · 동시 수락 (AC-18) ----

    @Test
    @DisplayName("수락은 약속을 잠금 조회하기 전에 글 G와 고른 첨부 글 S1을 잠근다")
    void handle_locksInvolvedPostsBeforeLockedLookup() {
        givenDefaultExchangeAndProposal();

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(1, postLockRepository.lockedPostIds().size());
        assertEquals(Set.of(POST_ID, SOURCE_POST_31), Set.copyOf(postLockRepository.lockedPostIds().get(0)));
        assertEquals(0, postLockRepository.lockedLookupCountsAtLock().get(0));
        assertTrue(appointmentRepository.lockedLookupCount() >= 1);
    }

    @Test
    @DisplayName("같은 글의 두 제안을 차례로 수락하면 먼저 처리된 1건만 확정되고 늦은 쪽은 EXCHANGE_108 · AUTO_REJECTED다")
    void handle_twoAcceptsOnSamePost_onlyFirstConfirms() {
        // given
        givenDefaultExchangeAndProposal();
        exchangeRepository.preload(Exchange.of(2L, POST_ID, OTHER_MEMBER_ID, OWNER_ID, OfferType.POST,
                List.of(postItem(41L, 601L, 0)), ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(appointment(12L, 2L, OTHER_MEMBER_ID, OWNER_ID, AppointmentStatus.PROPOSED,
                scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(41L)));

        // when
        handler.handle(command(APPOINTMENT_ID, OWNER_ID));
        ExchangeException late = assertThrows(ExchangeException.class,
                () -> handler.handle(command(12L, OWNER_ID)));

        // then
        assertEquals(ExchangeErrorCode.POST_RESERVED_ELSEWHERE, late.getErrorCode());
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(12L).getStatus());
        assertEquals(1, appointmentRepository.all().stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .count());
        assertEquals(1, postPort.reserveRequests().size());
        assertTrue(postLockRepository.lockedPostIds().get(1).contains(POST_ID));
    }

    // ---- 실패: 약속 · 권한 ----

    @Test
    @DisplayName("없는 약속이면 EXCHANGE_101")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        givenDefaultExchangeAndProposal();

        assertRejected(ExchangeErrorCode.APPOINTMENT_NOT_FOUND,
                () -> handler.handle(command(MISSING_APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("교환 참여자가 아닌 회원이 수락하면 EXCHANGE_002")
    void handle_thirdParty_throwsNotParticipant() {
        givenDefaultExchangeAndProposal();

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(APPOINTMENT_ID, OTHER_MEMBER_ID)));
    }

    @Test
    @DisplayName("참여자가 아니면 교환이 종료됐어도 EXCHANGE_002가 먼저다")
    void handle_thirdPartyOnClosedExchange_throwsNotParticipant() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(APPOINTMENT_ID, OTHER_MEMBER_ID)));
    }

    @Test
    @DisplayName("제안자가 자기 제안을 수락하면 EXCHANGE_104이고 PROPOSED 그대로다")
    void handle_byProposer_throwsNotReceiver() {
        givenDefaultExchangeAndProposal();

        assertRejected(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
    }

    @Test
    @DisplayName("제안자면 교환이 종료됐어도 EXCHANGE_104가 먼저다")
    void handle_byProposerOnClosedExchange_throwsNotReceiver() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertRejected(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
    }

    // ---- 실패: 교환 · 제안 상태 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 진행 중이 아니면 EXCHANGE_010이고 PROPOSED 그대로다")
    void handle_exchangeNotActive_throwsExchangeClosed(ExchangeStatus status) {
        exchangeRepository.preload(defaultExchange(status));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("교환이 종료됐고 제안도 거절된 상태면 EXCHANGE_010이 먼저다")
    void handle_closedExchangeAndRejectedProposal_throwsExchangeClosed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.REJECTED, scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_31)));

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
            names = {"REJECTED", "WITHDRAWN", "CONFIRMED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("진행 중이 아닌 제안을 수락하면 EXCHANGE_106이고 상태는 그대로다")
    void handle_proposalNotActive_throwsProposalNotActive(AppointmentStatus status) {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_31)));

        assertRejected(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("자동 거절된 제안을 수락하면 EXCHANGE_108이고 AUTO_REJECTED 그대로다")
    void handle_autoRejectedProposal_throwsPostReservedElsewhere() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.AUTO_REJECTED, scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_31)));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    // ---- 시각 ----

    @Test
    @DisplayName("약속 시각이 지금이면 EXCHANGE_107이고 PROPOSED 그대로다")
    void handle_scheduledAtNow_throwsScheduledAtPassed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW, NOW));

        assertRejected(ExchangeErrorCode.SCHEDULED_AT_PASSED,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("약속 시각이 1시간 지났으면 EXCHANGE_107이고 PROPOSED 그대로다")
    void handle_scheduledAtPassed_throwsScheduledAtPassed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        Instant oneHourAgo = NOW.minus(Duration.ofHours(1));
        appointmentRepository.preload(scheduledProposal(null, oneHourAgo));

        assertRejected(ExchangeErrorCode.SCHEDULED_AT_PASSED,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("옛 만료 시각이 지금이고 약속 시각은 1시간 뒤면 수락된다")
    void handle_oldExpiryNowBeforeScheduledAt_confirms() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW, NOW.plus(Duration.ofHours(1))));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("약속 시각이 3일 뒤인 제안을 제안 후 25시간에 수락하면 CONFIRMED다")
    void handle_proposal25HoursOld_confirms() {
        // given
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        Instant proposedAt = NOW.minus(Duration.ofHours(25));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, scheduledTime(NOW.plus(Duration.ofDays(3))), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), proposedAt.plus(Duration.ofHours(24)), 30, null, null,
                proposedAt, proposedAt));

        // when
        Long acceptedId = handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(APPOINTMENT_ID, acceptedId);
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(List.of(List.of(POST_ID, SOURCE_POST_31)), postPort.reserveRequests());
    }

    @Test
    @DisplayName("지금 바로 제안을 제안 후 25시간에 수락하면 CONFIRMED다")
    void handle_nowProposal25HoursOld_confirms() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.minus(Duration.ofHours(1)), List.of(ITEM_31)));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 실패: 다른 약속 · 글 상태 ----

    @Test
    @DisplayName("같은 글의 다른 교환에 확정 약속이 있으면 EXCHANGE_108이고 PROPOSED 그대로다")
    void handle_confirmedElsewhereOnSamePost_throwsPostReservedElsewhere() {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, 601L, 0)));
        appointmentRepository.preload(otherAppointment(12L, 2L, AppointmentStatus.CONFIRMED, List.of(41L)));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("글 G가 예약중이면 EXCHANGE_108")
    void handle_postReserved_throwsPostReservedElsewhere() {
        givenDefaultExchangeAndProposal();
        postPort.register(post(POST_ID, OWNER_ID, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"COMPLETED", "DELETED"})
    @DisplayName("글 G가 교환 완료 · 삭제면 EXCHANGE_005")
    void handle_postNotRequestable_throwsPostNotRequestable(PostInfo.Status status) {
        givenDefaultExchangeAndProposal();
        postPort.register(post(POST_ID, OWNER_ID, status));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("글 G를 찾을 수 없으면 EXCHANGE_005")
    void handle_postMissing_throwsPostNotRequestable() {
        Long missingPostId = 888L;
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, missingPostId, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(postItem(ITEM_31, SOURCE_POST_31, 0)), ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"RESERVED", "COMPLETED", "DELETED"})
    @DisplayName("고른 첨부 글 S1이 교환 불가면 EXCHANGE_112이고 PROPOSED 그대로다")
    void handle_chosenAttachedPostUnavailable_throwsItemNotAvailable(PostInfo.Status status) {
        givenDefaultExchangeAndProposal();
        postPort.register(post(SOURCE_POST_31, REQUESTER_ID, status));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("고른 첨부 글을 찾을 수 없으면 EXCHANGE_112")
    void handle_chosenAttachedPostMissing_throwsItemNotAvailable() {
        Long missingSourcePostId = 777L;
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(postItem(ITEM_31, missingSourcePostId, 0)), ExchangeStatus.ACTIVE, null,
                CREATED_AT, CREATED_AT));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("고르지 않은 첨부 글 S2가 예약중이어도 수락된다")
    void handle_unchosenAttachedPostReserved_confirms() {
        givenDefaultExchangeAndProposal();
        postPort.register(post(SOURCE_POST_32, REQUESTER_ID, PostInfo.Status.RESERVED));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 실패: 글 · 첨부 글이 다른 약속에 묶임 (AC-19) ----

    @Test
    @DisplayName("다른 글 교환의 확정 약속이 G를 첨부 글로 골랐으면 EXCHANGE_108")
    void handle_postChosenByOtherConfirmed_throwsPostReservedElsewhere() {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, OTHER_POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, POST_ID, 0)));
        appointmentRepository.preload(otherAppointment(12L, 2L, AppointmentStatus.CONFIRMED, List.of(41L)));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("G를 글로 둔 다른 교환에 완료 약속이 있으면 EXCHANGE_005")
    void handle_postOfOtherCompleted_throwsPostNotRequestable() {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, ExchangeStatus.COMPLETED, List.of(postItem(41L, 601L, 0)));
        appointmentRepository.preload(otherAppointment(12L, 2L, AppointmentStatus.COMPLETED, List.of(41L)));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("다른 글 교환의 완료 약속이 G를 첨부 글로 골랐으면 EXCHANGE_005")
    void handle_postChosenByOtherCompleted_throwsPostNotRequestable() {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, OTHER_POST_ID, ExchangeStatus.COMPLETED, List.of(postItem(41L, POST_ID, 0)));
        appointmentRepository.preload(otherAppointment(12L, 2L, AppointmentStatus.COMPLETED, List.of(41L)));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CONFIRMED", "COMPLETED"})
    @DisplayName("고른 첨부 글 S1을 글로 둔 다른 교환에 확정 · 완료 약속이 있으면 EXCHANGE_112")
    void handle_chosenAttachedPostIsPostOfOtherBound_throwsItemNotAvailable(AppointmentStatus status) {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(3L, SOURCE_POST_31, ExchangeStatus.ACTIVE, List.of(postItem(41L, 601L, 0)));
        appointmentRepository.preload(otherAppointment(13L, 3L, status, List.of(41L)));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CONFIRMED", "COMPLETED"})
    @DisplayName("다른 교환의 확정 · 완료 약속이 S1을 첨부 글로 골랐으면 EXCHANGE_112")
    void handle_chosenAttachedPostChosenByOtherBound_throwsItemNotAvailable(AppointmentStatus status) {
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(3L, OTHER_POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, SOURCE_POST_31, 0)));
        appointmentRepository.preload(otherAppointment(13L, 3L, status, List.of(41L)));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("S1이 다른 교환에 첨부만 됐고 그 약속이 PROPOSED이거나 S1을 고르지 않았으면 수락된다")
    void handle_attachedPostNotBound_confirms() {
        // given
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(3L, OTHER_POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, SOURCE_POST_31, 0)));
        preloadOtherExchange(4L, 12L, ExchangeStatus.ACTIVE,
                List.of(postItem(42L, SOURCE_POST_31, 0), postItem(43L, 603L, 1)));
        appointmentRepository.preload(otherAppointment(13L, 3L, AppointmentStatus.PROPOSED, List.of(41L)));
        appointmentRepository.preload(otherAppointment(14L, 4L, AppointmentStatus.CONFIRMED, List.of(43L)));

        // when
        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        // then
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 처리 실패 (AC-20) ----

    @Test
    @DisplayName("예약중 요청이 실패하면 수락은 실패하고 P와 다른 방 제안은 모두 PROPOSED 그대로다")
    void handle_reserveFails_leavesAllProposed() {
        // given
        givenDefaultExchangeAndProposal();
        preloadOtherExchange(2L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(41L, 601L, 0)));
        preloadOtherExchange(3L, POST_ID, ExchangeStatus.ACTIVE, List.of(postItem(42L, 602L, 0)));
        appointmentRepository.preload(otherProposal(12L, 2L, EXPIRES_AT, List.of(41L)));
        appointmentRepository.preload(otherProposal(13L, 3L, EXPIRES_AT, List.of(42L)));
        IllegalStateException reserveFailure = new IllegalStateException("post BC 예약중 전환 실패");
        postPort.failReserveWith(reserveFailure);

        // when
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));

        // then
        assertSame(reserveFailure, thrown);
        assertEquals(List.of(List.of(POST_ID, SOURCE_POST_31)), postPort.reserveRequests());
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
        assertNull(stored(APPOINTMENT_ID).getConfirmedAt());
        assertEquals(AppointmentStatus.PROPOSED, stored(12L).getStatus());
        assertEquals(AppointmentStatus.PROPOSED, stored(13L).getStatus());
    }

    // ---- helpers ----

    /** 거절되면 예외 코드가 맞고, 어떤 약속도 저장되지 않으며 예약중 요청도 가지 않는다. */
    private void assertRejected(ExchangeErrorCode expected, Executable action) {
        Appointment before = appointmentRepository.findById(APPOINTMENT_ID).orElse(null);
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
        assertEquals(0, appointmentRepository.saveCount());
        assertTrue(postPort.reserveRequests().isEmpty());
        if (before != null) {
            assertEquals(before.getStatus(), stored(APPOINTMENT_ID).getStatus());
        }
    }

    private void givenDefaultExchangeAndProposal() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));
    }

    private Appointment stored(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    private void preloadOtherExchange(Long exchangeId, Long postId, ExchangeStatus status, List<OfferedItem> items) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CREATED_AT : null;
        exchangeRepository.preload(Exchange.of(exchangeId, postId, OTHER_MEMBER_ID + exchangeId, OWNER_ID + exchangeId,
                OfferType.POST, items, status, completedAt, CREATED_AT, CREATED_AT));
    }

    private static AcceptAppointmentCommand command(Long appointmentId, Long accepterId) {
        return new AcceptAppointmentCommand(appointmentId, accepterId);
    }

    /** 교환 E: 글 G · 첨부 글 S1(ITEM_31) · S2(ITEM_32) */
    private static Exchange defaultExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CREATED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(postItem(ITEM_31, SOURCE_POST_31, 0), postItem(ITEM_32, SOURCE_POST_32, 1)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    /** A가 B에게 제안한 SCHEDULED 약속 P (ITEM_31 선택 · 제안자 알림 30) */
    private static Appointment scheduledProposal(Instant expiresAt, Instant scheduledAt) {
        return appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.PROPOSED,
                scheduledTime(scheduledAt), expiresAt, List.of(ITEM_31));
    }

    private static Appointment otherProposal(Long id, Long exchangeId, Instant expiresAt, List<Long> itemIds) {
        return appointment(id, exchangeId, OTHER_MEMBER_ID + exchangeId, OWNER_ID + exchangeId,
                AppointmentStatus.PROPOSED, scheduledTime(SCHEDULED_AT), expiresAt, itemIds);
    }

    private static Appointment otherAppointment(Long id, Long exchangeId, AppointmentStatus status,
                                                List<Long> itemIds) {
        return appointment(id, exchangeId, OTHER_MEMBER_ID + exchangeId, OWNER_ID + exchangeId, status,
                scheduledTime(SCHEDULED_AT), EXPIRES_AT, itemIds);
    }

    private static Appointment appointment(Long id, Long exchangeId, Long proposerId, Long receiverId,
                                           AppointmentStatus status, AppointmentTime time, Instant expiresAt,
                                           List<Long> itemIds) {
        boolean confirmedBefore = status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELED || status == AppointmentStatus.NOT_MET;
        Instant confirmedAt = confirmedBefore ? CREATED_AT : null;
        return Appointment.of(id, exchangeId, proposerId, receiverId, status, time, PLACE,
                new AppointmentItemIds(itemIds), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduledTime(Instant scheduledAt) {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt);
    }

    private static OfferedItem postItem(Long itemId, Long sourcePostId, int order) {
        return OfferedItem.of(itemId, sourcePostId, null, null, null, order);
    }

    private static PostInfo post(Long id, Long authorId, PostInfo.Status status) {
        return new PostInfo(id, "글 " + id, "https://cdn.example.com/post/" + id + "/1.jpg", authorId, status);
    }
}
