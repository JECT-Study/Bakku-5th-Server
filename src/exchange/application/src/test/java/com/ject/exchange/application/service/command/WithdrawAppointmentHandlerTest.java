package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.command.RejectAppointmentCommand;
import com.ject.exchange.application.dto.command.WithdrawAppointmentCommand;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.dto.query.GetCurrentAppointmentQuery;
import com.ject.exchange.application.service.query.GetAppointmentHandler;
import com.ject.exchange.application.service.query.GetCurrentAppointmentHandler;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class WithdrawAppointmentHandlerTest {

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
    /** 같은 글 G의 다른 교환 E2 */
    private static final Long OTHER_EXCHANGE_ID = 2L;
    /** 첨부 글 S1을 본 글로 하는 교환 E3 */
    private static final Long SOURCE_POST_EXCHANGE_ID = 3L;
    /** 글 G */
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    private static final Long ITEM_32 = 32L;
    private static final Long ITEM_41 = 41L;
    private static final Long ITEM_71 = 71L;
    /** 첨부 글 S1 (ITEM_31의 원본) */
    private static final Long SOURCE_POST_31 = 501L;
    /** 첨부 글 S2 (ITEM_32의 원본) */
    private static final Long SOURCE_POST_32 = 502L;
    private static final Long SOURCE_POST_41 = 601L;
    private static final Long SOURCE_POST_71 = 701L;
    private static final Long OTHER_APPOINTMENT_ID = 12L;
    private static final Long SOURCE_POST_APPOINTMENT_ID = 13L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakePostPort postPort;
    private ServiceTime serviceTime;
    private WithdrawAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        postPort = new FakePostPort();
        serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new WithdrawAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                serviceTime);

        postPort.register(post(POST_ID, OWNER_ID));
        postPort.register(post(SOURCE_POST_31, REQUESTER_ID));
        postPort.register(post(SOURCE_POST_32, REQUESTER_ID));
        postPort.register(post(SOURCE_POST_41, OTHER_MEMBER_ID));
        postPort.register(post(SOURCE_POST_71, OTHER_MEMBER_ID));
    }

    // ---- 철회 (AC-1) ----

    @Test
    @DisplayName("제안자가 철회하면 WITHDRAWN으로 저장되고 시간 · 장소 · 아이템 · 알림 · 만료 · 확정 시각은 그대로다")
    void handle_byProposer_savesWithdrawn() {
        // given
        givenDefaultExchangeAndProposal();

        // when
        Long withdrawnId = handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        // then
        assertEquals(APPOINTMENT_ID, withdrawnId);
        assertEquals(1, appointmentRepository.saveCount());
        Appointment saved = stored(APPOINTMENT_ID);
        assertEquals(AppointmentStatus.WITHDRAWN, saved.getStatus());
        assertEquals(EXCHANGE_ID, saved.getExchangeId());
        assertEquals(REQUESTER_ID, saved.getProposerUserId());
        assertEquals(OWNER_ID, saved.getReceiverUserId());
        assertEquals(AppointmentTimeType.SCHEDULED, saved.getTime().type());
        assertEquals(SCHEDULED_AT, saved.getTime().scheduledAt());
        assertEquals(PLACE, saved.getPlace());
        assertEquals(List.of(ITEM_31), saved.getItemIds().values());
        assertEquals(30, saved.getProposerReminderOffsetMinutes());
        assertNull(saved.getReceiverReminderOffsetMinutes());
        assertEquals(EXPIRES_AT, saved.getExpiresAt());
        assertNull(saved.getConfirmedAt());
    }

    @Test
    @DisplayName("철회 후 철회자가 조회하면 WITHDRAWN · 확정 null · 만료 null · 내 알림 30이다")
    void handle_thenGetAppointment_returnsWithdrawnForWithdrawer() {
        // given
        givenDefaultExchangeAndProposal();
        GetAppointmentHandler getHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        // when
        Long withdrawnId = handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));
        AppointmentResult result = getHandler.handle(new GetAppointmentQuery(withdrawnId, REQUESTER_ID));

        // then
        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(AppointmentStatus.WITHDRAWN, result.status());
        assertNull(result.confirmedAt());
        assertNull(result.expiresAt());
        assertEquals(30, result.myReminderOffsetMinutes());
    }

    @Test
    @DisplayName("만료 시각 1초 전에 철회하면 WITHDRAWN이다")
    void handle_justBeforeExpiry_withdraws() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW.plusSeconds(1), SCHEDULED_AT));

        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("만료 전 지금 바로 약속을 철회하면 WITHDRAWN이다")
    void handle_nowAppointment_withdraws() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.plus(Duration.ofHours(23)), List.of(ITEM_31)));

        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("게시자가 보낸 제안도 게시자가 철회하면 WITHDRAWN이다")
    void handle_ownerWithdrawsOwnProposal_withdraws() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, OWNER_ID, REQUESTER_ID,
                AppointmentStatus.PROPOSED, scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_31)));

        handler.handle(command(APPOINTMENT_ID, OWNER_ID));

        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 철회 뒤 새로 제안 (AC-2) ----

    @Test
    @DisplayName("철회 뒤 교환의 현재 유효한 약속은 없다")
    void handle_thenGetCurrentAppointment_returnsEmpty() {
        // given
        givenDefaultExchangeAndProposal();
        GetCurrentAppointmentHandler currentHandler = new GetCurrentAppointmentHandler(appointmentRepository,
                exchangeRepository, postPort, serviceTime);

        // when
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));
        Optional<AppointmentResult> forRequester = currentHandler.handle(
                new GetCurrentAppointmentQuery(EXCHANGE_ID, REQUESTER_ID));
        Optional<AppointmentResult> forOwner = currentHandler.handle(
                new GetCurrentAppointmentQuery(EXCHANGE_ID, OWNER_ID));

        // then
        assertTrue(forRequester.isEmpty());
        assertTrue(forOwner.isEmpty());
    }

    @Test
    @DisplayName("철회 뒤 제안자가 같은 교환에서 새로 제안하면 EXCHANGE_103 없이 새 PROPOSED 약속이 만들어진다")
    void handle_thenProposerProposesAgain_createsNewProposal() {
        // given
        givenDefaultExchangeAndProposal();

        // when
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));
        Long newId = proposeHandler().handle(proposeCommand(REQUESTER_ID));

        // then
        assertNotEquals(APPOINTMENT_ID, newId);
        Appointment created = stored(newId);
        assertEquals(AppointmentStatus.PROPOSED, created.getStatus());
        assertEquals(REQUESTER_ID, created.getProposerUserId());
        assertEquals(OWNER_ID, created.getReceiverUserId());
        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("철회 뒤 받는 사람이 같은 교환에서 제안하면 EXCHANGE_103 없이 새 PROPOSED 약속이 만들어진다")
    void handle_thenReceiverProposes_createsNewProposal() {
        // given
        givenDefaultExchangeAndProposal();

        // when
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));
        Long newId = proposeHandler().handle(proposeCommand(OWNER_ID));

        // then
        Appointment created = stored(newId);
        assertEquals(AppointmentStatus.PROPOSED, created.getStatus());
        assertEquals(OWNER_ID, created.getProposerUserId());
        assertEquals(REQUESTER_ID, created.getReceiverUserId());
    }

    @Test
    @DisplayName("철회하지 않고 진행 중 제안이 있는데 새로 제안하면 EXCHANGE_103이다")
    void propose_withoutWithdraw_throwsAppointmentAlreadyExists() {
        givenDefaultExchangeAndProposal();

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> proposeHandler().handle(proposeCommand(REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 다른 방 · 글에 영향 없음 (AC-3) ----

    @Test
    @DisplayName("철회해도 같은 글의 다른 교환 제안은 PROPOSED · 두 교환은 ACTIVE 그대로이고 예약중 요청은 없다")
    void handle_otherExchangeOnSamePost_untouched() {
        // given
        givenDefaultExchangeAndProposal();
        exchangeRepository.preload(otherExchangeOnSamePost());
        appointmentRepository.preload(otherProposalOnSamePost());

        // when
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        // then
        assertEquals(AppointmentStatus.PROPOSED, stored(OTHER_APPOINTMENT_ID).getStatus());
        assertEquals(ExchangeStatus.ACTIVE, exchangeRepository.findById(EXCHANGE_ID).orElseThrow().getStatus());
        assertEquals(ExchangeStatus.ACTIVE, exchangeRepository.findById(OTHER_EXCHANGE_ID).orElseThrow().getStatus());
        assertEquals(1, appointmentRepository.saveCount());
        assertEquals(0, exchangeRepository.saveCount());
        assertTrue(postPort.reserveRequests().isEmpty());
    }

    // ---- 없는 약속 (AC-5) ----

    @Test
    @DisplayName("없는 약속이면 EXCHANGE_101")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        givenDefaultExchangeAndProposal();

        assertWithdrawFails(ExchangeErrorCode.APPOINTMENT_NOT_FOUND,
                () -> handler.handle(command(MISSING_APPOINTMENT_ID, REQUESTER_ID)));
    }

    // ---- 참여자 아님 (AC-6) ----

    @Test
    @DisplayName("교환 참여자가 아닌 회원이 철회하면 EXCHANGE_002이고 PROPOSED 그대로다")
    void handle_thirdParty_throwsNotParticipant() {
        givenDefaultExchangeAndProposal();

        assertWithdrawFails(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(APPOINTMENT_ID, OTHER_MEMBER_ID)));
    }

    @Test
    @DisplayName("참여자가 아니면 교환이 종료됐어도 EXCHANGE_002가 먼저다")
    void handle_thirdPartyOnClosedExchange_throwsNotParticipant() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertWithdrawFails(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(command(APPOINTMENT_ID, OTHER_MEMBER_ID)));
    }

    // ---- 받는 사람이 철회 (AC-7) ----

    @Test
    @DisplayName("받는 사람이 철회하면 EXCHANGE_105이고 PROPOSED 그대로다")
    void handle_byReceiver_throwsNotProposer() {
        givenDefaultExchangeAndProposal();

        assertWithdrawFails(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("받는 사람이면 교환이 종료됐어도 EXCHANGE_105가 먼저다")
    void handle_byReceiverOnClosedExchange_throwsNotProposer() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertWithdrawFails(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    @Test
    @DisplayName("받는 사람이면 제안이 이미 끝났어도 EXCHANGE_105가 먼저다")
    void handle_byReceiverOnRejectedProposal_throwsNotProposer() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.REJECTED, scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_31)));

        assertWithdrawFails(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER,
                () -> handler.handle(command(APPOINTMENT_ID, OWNER_ID)));
    }

    // ---- 진행 중이 아닌 제안 (AC-8) ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REJECTED", "WITHDRAWN", "EXPIRED", "AUTO_REJECTED",
            "CONFIRMED", "CANCELED", "COMPLETED", "NOT_MET"})
    @DisplayName("진행 중이 아닌 제안을 철회하면 EXCHANGE_106이고 상태는 그대로다")
    void handle_proposalNotActive_throwsProposalNotActive(AppointmentStatus status) {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_31)));

        assertWithdrawFails(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
        assertEquals(status, stored(APPOINTMENT_ID).getStatus());
    }

    // ---- 만료된 제안 (AC-9) ----

    @Test
    @DisplayName("만료 시각이 지금이면 EXCHANGE_106이고 저장 상태는 PROPOSED 그대로다")
    void handle_expiresAtNow_throwsProposalNotActive() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW, NOW.plus(Duration.ofHours(1))));

        assertWithdrawFails(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("만료 시각과 약속 시각이 지났으면 EXCHANGE_106")
    void handle_expiredAndScheduledAtPassed_throwsProposalNotActive() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        Instant oneHourAgo = NOW.minus(Duration.ofHours(1));
        appointmentRepository.preload(scheduledProposal(oneHourAgo, oneHourAgo));

        assertWithdrawFails(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
    }

    @Test
    @DisplayName("만료 시각과 약속 시각이 모두 지금이면 EXCHANGE_106")
    void handle_expiredAndScheduledAtNow_throwsProposalNotActive() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW, NOW));

        assertWithdrawFails(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
    }

    @Test
    @DisplayName("지금 바로 약속의 만료 시각이 지났으면 EXCHANGE_106")
    void handle_expiredNowAppointment_throwsProposalNotActive() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, new AppointmentTime(AppointmentTimeType.NOW, null),
                NOW.minus(Duration.ofMinutes(1)), List.of(ITEM_31)));

        assertWithdrawFails(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
    }

    @Test
    @DisplayName("만료된 제안이 있으면 철회 없이도 새로 제안할 수 있다")
    void propose_afterExpiredProposal_createsNewProposal() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(NOW, NOW.plus(Duration.ofHours(1))));

        Long newId = proposeHandler().handle(proposeCommand(REQUESTER_ID));

        assertNotEquals(APPOINTMENT_ID, newId);
        assertEquals(AppointmentStatus.PROPOSED, stored(newId).getStatus());
    }

    // ---- 종료된 교환 (AC-10) ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 진행 중이 아니면 EXCHANGE_010이고 PROPOSED 그대로다")
    void handle_exchangeNotActive_throwsExchangeClosed(ExchangeStatus status) {
        exchangeRepository.preload(defaultExchange(status));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));

        assertWithdrawFails(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("교환이 종료됐고 제안도 만료됐으면 EXCHANGE_010이 먼저다")
    void handle_closedExchangeAndExpiredProposal_throwsExchangeClosed() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.CLOSED));
        appointmentRepository.preload(scheduledProposal(NOW, NOW.plus(Duration.ofHours(1))));

        assertWithdrawFails(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));
    }

    // ---- 잠금 · 동시 변경 (AC-11) ----

    @Test
    @DisplayName("철회는 약속을 잠금 조회하기 전에 글 G와 고른 첨부 글 S1을 잠근다")
    void handle_locksInvolvedPostsBeforeLockedLookup() {
        givenDefaultExchangeAndProposal();

        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(1, postLockRepository.lockedPostIds().size());
        assertEquals(Set.of(POST_ID, SOURCE_POST_31), Set.copyOf(postLockRepository.lockedPostIds().get(0)));
        assertEquals(0, postLockRepository.lockedLookupCountsAtLock().get(0));
        assertTrue(appointmentRepository.lockedLookupCount() >= 1);
    }

    @Test
    @DisplayName("같은 제안을 수락한 뒤 철회하면 EXCHANGE_106이고 CONFIRMED 그대로다")
    void handle_withdrawAfterAcceptOnSameProposal_throwsProposalNotActive() {
        givenDefaultExchangeAndProposal();
        acceptHandler().handle(new AcceptAppointmentCommand(APPOINTMENT_ID, OWNER_ID, null));

        ExchangeException late = assertThrows(ExchangeException.class,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, late.getErrorCode());
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("같은 제안을 철회한 뒤 수락하면 늦은 수락은 EXCHANGE_106이고 WITHDRAWN 그대로다")
    void handle_acceptAfterWithdrawOnSameProposal_throwsProposalNotActive() {
        givenDefaultExchangeAndProposal();
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        ExchangeException late = assertThrows(ExchangeException.class,
                () -> acceptHandler().handle(new AcceptAppointmentCommand(APPOINTMENT_ID, OWNER_ID, null)));

        assertEquals(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, late.getErrorCode());
        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
        assertTrue(postPort.reserveRequests().isEmpty());
    }

    @Test
    @DisplayName("철회가 먼저고 다른 방 수락이 뒤면 P는 WITHDRAWN · P2는 CONFIRMED다")
    void handle_withdrawBeforeOtherAccept_keepsWithdrawn() {
        // given
        givenDefaultExchangeAndProposal();
        exchangeRepository.preload(otherExchangeOnSamePost());
        appointmentRepository.preload(otherProposalOnSamePost());

        // when
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));
        acceptHandler().handle(new AcceptAppointmentCommand(OTHER_APPOINTMENT_ID, OWNER_ID, null));

        // then
        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, stored(OTHER_APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("다른 방 수락이 먼저고 철회가 뒤면 늦은 철회는 EXCHANGE_106이고 P는 AUTO_REJECTED 그대로다")
    void handle_withdrawAfterOtherAccept_throwsProposalNotActive() {
        // given
        givenDefaultExchangeAndProposal();
        exchangeRepository.preload(otherExchangeOnSamePost());
        appointmentRepository.preload(otherProposalOnSamePost());
        acceptHandler().handle(new AcceptAppointmentCommand(OTHER_APPOINTMENT_ID, OWNER_ID, null));

        // when
        ExchangeException late = assertThrows(ExchangeException.class,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));

        // then
        assertEquals(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, late.getErrorCode());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, stored(OTHER_APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("같은 제안을 거절한 뒤 철회하면 EXCHANGE_106이고 REJECTED 그대로다")
    void handle_withdrawAfterRejectOnSameProposal_throwsProposalNotActive() {
        givenDefaultExchangeAndProposal();
        rejectHandler().handle(new RejectAppointmentCommand(APPOINTMENT_ID, OWNER_ID));

        ExchangeException late = assertThrows(ExchangeException.class,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, late.getErrorCode());
        assertEquals(AppointmentStatus.REJECTED, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("같은 제안을 철회한 뒤 거절하면 늦은 거절은 EXCHANGE_106이고 WITHDRAWN 그대로다")
    void handle_rejectAfterWithdrawOnSameProposal_throwsProposalNotActive() {
        givenDefaultExchangeAndProposal();
        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));

        ExchangeException late = assertThrows(ExchangeException.class,
                () -> rejectHandler().handle(new RejectAppointmentCommand(APPOINTMENT_ID, OWNER_ID)));

        assertEquals(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, late.getErrorCode());
        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("고른 첨부 글 S1의 다른 교환 수락이 먼저면 늦은 철회는 EXCHANGE_106이고 P는 AUTO_REJECTED 그대로다")
    void handle_withdrawAfterAcceptOnChosenAttachedPost_throwsProposalNotActive() {
        // given
        givenDefaultExchangeAndProposal();
        exchangeRepository.preload(sourcePostExchange());
        appointmentRepository.preload(sourcePostProposal());
        acceptHandler().handle(new AcceptAppointmentCommand(SOURCE_POST_APPOINTMENT_ID, REQUESTER_ID, null));

        // when
        ExchangeException late = assertThrows(ExchangeException.class,
                () -> handler.handle(command(APPOINTMENT_ID, REQUESTER_ID)));

        // then
        assertEquals(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, late.getErrorCode());
        assertEquals(AppointmentStatus.AUTO_REJECTED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, stored(SOURCE_POST_APPOINTMENT_ID).getStatus());
    }

    @Test
    @DisplayName("철회가 먼저면 고른 첨부 글 S1의 다른 교환 수락 뒤에도 P는 WITHDRAWN이다")
    void handle_withdrawBeforeAcceptOnChosenAttachedPost_keepsWithdrawn() {
        givenDefaultExchangeAndProposal();
        exchangeRepository.preload(sourcePostExchange());
        appointmentRepository.preload(sourcePostProposal());

        handler.handle(command(APPOINTMENT_ID, REQUESTER_ID));
        acceptHandler().handle(new AcceptAppointmentCommand(SOURCE_POST_APPOINTMENT_ID, REQUESTER_ID, null));

        assertEquals(AppointmentStatus.WITHDRAWN, stored(APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, stored(SOURCE_POST_APPOINTMENT_ID).getStatus());
    }

    // ---- helpers ----

    /** 철회가 막히면 예외 코드가 맞고, 어떤 약속 · 교환도 저장되지 않으며 예약중 요청도 가지 않는다. */
    private void assertWithdrawFails(ExchangeErrorCode expected, Executable action) {
        Appointment before = appointmentRepository.findById(APPOINTMENT_ID).orElse(null);
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(0, exchangeRepository.saveCount());
        assertTrue(postPort.reserveRequests().isEmpty());
        if (before != null) {
            assertEquals(before.getStatus(), stored(APPOINTMENT_ID).getStatus());
        }
    }

    private AcceptAppointmentHandler acceptHandler() {
        return new AcceptAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, serviceTime);
    }

    private RejectAppointmentHandler rejectHandler() {
        return new RejectAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                serviceTime);
    }

    private ProposeAppointmentHandler proposeHandler() {
        return new ProposeAppointmentHandler(exchangeRepository, appointmentRepository, postPort,
                new FakeUserPort(), serviceTime);
    }

    private void givenDefaultExchangeAndProposal() {
        exchangeRepository.preload(defaultExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(scheduledProposal(EXPIRES_AT, SCHEDULED_AT));
    }

    private Appointment stored(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    private static WithdrawAppointmentCommand command(Long appointmentId, Long withdrawerId) {
        return new WithdrawAppointmentCommand(appointmentId, withdrawerId);
    }

    private static ProposeAppointmentCommand proposeCommand(Long proposerId) {
        return new ProposeAppointmentCommand(EXCHANGE_ID, proposerId, AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                PLACE, List.of(ITEM_32), 30);
    }

    /** 교환 E: 글 G · 첨부 글 S1(ITEM_31) · S2(ITEM_32) */
    private static Exchange defaultExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CREATED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(postItem(ITEM_31, SOURCE_POST_31, 0), postItem(ITEM_32, SOURCE_POST_32, 1)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    /** 같은 글 G의 다른 교환 E2: 요청자 C · 게시자 B */
    private static Exchange otherExchangeOnSamePost() {
        return Exchange.of(OTHER_EXCHANGE_ID, POST_ID, OTHER_MEMBER_ID, OWNER_ID, OfferType.POST,
                List.of(postItem(ITEM_41, SOURCE_POST_41, 0)), ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);
    }

    /** E2에서 C가 B에게 제안한 만료 전 제안 P2 */
    private static Appointment otherProposalOnSamePost() {
        return appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID, OTHER_MEMBER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_41));
    }

    /** 첨부 글 S1을 본 글로 하는 교환 E3: 요청자 C · 게시자 A(S1 작성자) */
    private static Exchange sourcePostExchange() {
        return Exchange.of(SOURCE_POST_EXCHANGE_ID, SOURCE_POST_31, OTHER_MEMBER_ID, REQUESTER_ID, OfferType.POST,
                List.of(postItem(ITEM_71, SOURCE_POST_71, 0)), ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);
    }

    /** E3에서 C가 A에게 제안한 만료 전 제안 P3 */
    private static Appointment sourcePostProposal() {
        return appointment(SOURCE_POST_APPOINTMENT_ID, SOURCE_POST_EXCHANGE_ID, OTHER_MEMBER_ID, REQUESTER_ID,
                AppointmentStatus.PROPOSED, scheduledTime(SCHEDULED_AT), EXPIRES_AT, List.of(ITEM_71));
    }

    /** A가 B에게 제안한 SCHEDULED 약속 P (ITEM_31 선택 · 제안자 알림 30) */
    private static Appointment scheduledProposal(Instant expiresAt, Instant scheduledAt) {
        return appointment(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.PROPOSED,
                scheduledTime(scheduledAt), expiresAt, List.of(ITEM_31));
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

    private static PostInfo post(Long id, Long authorId) {
        return new PostInfo(id, "글 " + id, "https://cdn.example.com/post/" + id + "/1.jpg", authorId,
                PostInfo.Status.AVAILABLE);
    }
}
