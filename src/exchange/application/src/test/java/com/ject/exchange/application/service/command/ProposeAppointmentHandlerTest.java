package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ProposeAppointmentHandlerTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long MISSING_EXCHANGE_ID = 999L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    private static final Long ITEM_32 = 32L;
    private static final Long FOREIGN_ITEM = 99L;
    private static final Long SOURCE_POST_31 = 501L;
    private static final Long SOURCE_POST_32 = 502L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private ServiceTime serviceTime;
    private ProposeAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new ProposeAppointmentHandler(exchangeRepository, appointmentRepository, postPort, userPort,
                serviceTime);

        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(post(SOURCE_POST_31, "우사기 미니 피규어", REQUESTER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(post(SOURCE_POST_32, "하치와레 키링", REQUESTER_ID, PostInfo.Status.AVAILABLE));
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("요청자가 시간 지정으로 제안하면 PROPOSED · 제안자 알림 30 · 만료 시각 null로 저장되고 그 id를 반환한다")
    void handle_requesterScheduled_savesProposedAppointment() {
        // given
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        // when
        Long appointmentId = handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31)));

        // then
        assertNotNull(appointmentId);
        assertEquals(1, appointmentRepository.saveCount());
        Appointment saved = appointmentRepository.findById(appointmentId).orElseThrow();
        assertEquals(EXCHANGE_ID, saved.getExchangeId());
        assertEquals(REQUESTER_ID, saved.getProposerUserId());
        assertEquals(OWNER_ID, saved.getReceiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, saved.getStatus());
        assertEquals(AppointmentTimeType.SCHEDULED, saved.getTime().type());
        assertEquals(SCHEDULED_AT, saved.getTime().scheduledAt());
        assertEquals(PLACE, saved.getPlace());
        assertEquals(List.of(ITEM_31), saved.getItemIds().values());
        assertNull(saved.getExpiresAt());
        assertEquals(30, saved.getProposerReminderOffsetMinutes());
        assertNull(saved.getReceiverReminderOffsetMinutes());
        assertNull(saved.getConfirmedAt());
    }

    @Test
    @DisplayName("제안 후 조회하면 약속 응답 값이 제안 내용과 같고 내 알림 30 · 만료 시각 null이다")
    void handle_thenGetAppointment_returnsProposedValues() {
        // given
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        GetAppointmentHandler getHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        // when
        Long appointmentId = handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31)));
        AppointmentResult result = getHandler.handle(new GetAppointmentQuery(appointmentId, REQUESTER_ID));

        // then
        assertEquals(appointmentId, result.id());
        assertEquals(EXCHANGE_ID, result.exchangeId());
        assertEquals(REQUESTER_ID, result.proposerUserId());
        assertEquals(OWNER_ID, result.receiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertEquals(AppointmentTimeType.SCHEDULED, result.timeType());
        assertEquals(SCHEDULED_AT, result.scheduledAt());
        assertEquals(PLACE, result.place());
        assertEquals(List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어",
                thumbnailOf(SOURCE_POST_31))), result.items());
        assertNull(result.expiresAt());
        assertEquals(30, result.myReminderOffsetMinutes());
        assertFalse(result.otherActiveProposalsOnPost());
        assertNull(result.confirmedAt());
        assertEquals(NOW, result.createdAt());
    }

    @Test
    @DisplayName("게시자가 제안하면 제안자는 게시자 · 받는 사람은 요청자다")
    void handle_byOwner_proposerIsOwner() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        Long appointmentId = handler.handle(scheduledCommand(OWNER_ID, List.of(ITEM_31)));

        Appointment saved = appointmentRepository.findById(appointmentId).orElseThrow();
        assertEquals(OWNER_ID, saved.getProposerUserId());
        assertEquals(REQUESTER_ID, saved.getReceiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, saved.getStatus());
    }

    @Test
    @DisplayName("지금 바로 제안이면 시각 없음 · 만료 시각 null · 제안자 알림 null이다")
    void handle_now_savesWithoutScheduledAtAndReminder() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        Long appointmentId = handler.handle(command(REQUESTER_ID, AppointmentTimeType.NOW, null,
                List.of(ITEM_31)));

        Appointment saved = appointmentRepository.findById(appointmentId).orElseThrow();
        assertEquals(AppointmentTimeType.NOW, saved.getTime().type());
        assertNull(saved.getTime().scheduledAt());
        assertNull(saved.getExpiresAt());
        assertNull(saved.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("지금 바로 제안에 시각을 같이 보내도 저장되고 시각은 null이다")
    void handle_nowWithScheduledAt_ignoresScheduledAt() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        Long appointmentId = handler.handle(command(REQUESTER_ID, AppointmentTimeType.NOW, SCHEDULED_AT,
                List.of(ITEM_31)));

        Appointment saved = appointmentRepository.findById(appointmentId).orElseThrow();
        assertEquals(AppointmentTimeType.NOW, saved.getTime().type());
        assertNull(saved.getTime().scheduledAt());
        assertNull(saved.getExpiresAt());
    }

    @Test
    @DisplayName("약속 시각이 1시간 뒤여도 만료 시각 null · 제안자 알림 30이다")
    void handle_scheduledWithinDay_noExpiryAndFixedReminder() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        Instant oneHourLater = Instant.parse("2026-10-26T06:00:00Z");

        Long appointmentId = handler.handle(command(REQUESTER_ID, AppointmentTimeType.SCHEDULED, oneHourLater,
                List.of(ITEM_31)));

        Appointment saved = appointmentRepository.findById(appointmentId).orElseThrow();
        assertNull(saved.getExpiresAt());
        assertEquals(30, saved.getProposerReminderOffsetMinutes());
    }

    @Test
    @DisplayName("7일 후 마지막 칸(11-02 23:50)으로 제안하면 저장된다")
    void handle_lastSlotOfSeventhDay_saves() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        Instant lastSlot = Instant.parse("2026-11-02T14:50:00Z");

        Long appointmentId = handler.handle(command(REQUESTER_ID, AppointmentTimeType.SCHEDULED, lastSlot,
                List.of(ITEM_31)));

        assertEquals(lastSlot, appointmentRepository.findById(appointmentId).orElseThrow().getTime().scheduledAt());
    }

    @Test
    @DisplayName("약속 시각이 20분 뒤라 알림 시각이 이미 지났어도 제안자 알림 30으로 저장된다")
    void handle_reminderAlreadyPassed_savesThirty() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        Long appointmentId = handler.handle(command(REQUESTER_ID, AppointmentTimeType.SCHEDULED,
                Instant.parse("2026-10-26T05:20:00Z"), List.of(ITEM_31)));

        assertEquals(30, appointmentRepository.findById(appointmentId).orElseThrow()
                .getProposerReminderOffsetMinutes());
    }

    // ---- 아이템 자동 선택 · 선택 ----

    @Test
    @DisplayName("교환 가능한 POST 첨부가 1개면 생략해도 그 아이템이 선택된다")
    void handle_singleAvailablePostItemOmitted_autoSelects() {
        exchangeRepository.preload(postItemExchange(ExchangeStatus.ACTIVE,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0))));

        Long appointmentId = handler.handle(scheduledCommand(REQUESTER_ID, null));

        assertEquals(List.of(ITEM_31), appointmentRepository.findById(appointmentId).orElseThrow()
                .getItemIds().values());
    }

    @Test
    @DisplayName("NEW 첨부 1개면 생략해도 그 아이템이 선택된다")
    void handle_newItemOmitted_autoSelects() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        Long appointmentId = handler.handle(scheduledCommand(REQUESTER_ID, null));

        assertEquals(List.of(ITEM_31), appointmentRepository.findById(appointmentId).orElseThrow()
                .getItemIds().values());
    }

    @Test
    @DisplayName("첨부 2개 중 1개만 교환 가능하면 생략해도 그 아이템이 선택된다")
    void handle_oneOfTwoAvailableOmitted_autoSelectsAvailable() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(SOURCE_POST_32, "하치와레 키링", REQUESTER_ID, PostInfo.Status.RESERVED));

        Long appointmentId = handler.handle(scheduledCommand(REQUESTER_ID, null));

        assertEquals(List.of(ITEM_31), appointmentRepository.findById(appointmentId).orElseThrow()
                .getItemIds().values());
    }

    @Test
    @DisplayName("교환 가능한 첨부 2개를 모두 고르면 둘 다 저장된다")
    void handle_bothItemsSelected_savesBoth() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        Long appointmentId = handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31, ITEM_32)));

        assertEquals(List.of(ITEM_31, ITEM_32), appointmentRepository.findById(appointmentId).orElseThrow()
                .getItemIds().values());
    }

    @Test
    @DisplayName("교환 가능한 첨부가 2개인데 생략하면 INVALID_OFFERED_ITEMS이고 저장하지 않는다")
    void handle_twoAvailableOmitted_throwsInvalidOfferedItems() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_OFFERED_ITEMS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, null)));
    }

    @Test
    @DisplayName("이 교환의 첨부가 아닌 ID를 고르면 INVALID_OFFERED_ITEMS이고 저장하지 않는다")
    void handle_foreignItem_throwsInvalidOfferedItems() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_OFFERED_ITEMS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31, FOREIGN_ITEM))));
    }

    @Test
    @DisplayName("같은 ID를 두 번 고르면 INVALID_OFFERED_ITEMS이고 저장하지 않는다")
    void handle_duplicatedItem_throwsInvalidOfferedItems() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_OFFERED_ITEMS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31, ITEM_31))));
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"RESERVED", "COMPLETED", "DELETED"})
    @DisplayName("고른 POST 첨부의 원본 글이 예약중 · 교환 완료 · 삭제면 ITEM_NOT_AVAILABLE이고 저장하지 않는다")
    void handle_selectedItemNotAvailable_throwsItemNotAvailable(PostInfo.Status status) {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(SOURCE_POST_31, "우사기 미니 피규어", REQUESTER_ID, status));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("고른 2개 중 하나가 교환 불가면 ITEM_NOT_AVAILABLE이고 저장하지 않는다")
    void handle_oneOfSelectedNotAvailable_throwsItemNotAvailable() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(SOURCE_POST_32, "하치와레 키링", REQUESTER_ID, PostInfo.Status.COMPLETED));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31, ITEM_32))));
    }

    @Test
    @DisplayName("고른 POST 첨부의 원본 글이 조회되지 않으면 ITEM_NOT_AVAILABLE이고 저장하지 않는다")
    void handle_selectedItemSourceMissing_throwsItemNotAvailable() {
        Long missingSourcePostId = 777L;
        exchangeRepository.preload(postItemExchange(ExchangeStatus.ACTIVE,
                List.of(OfferedItem.of(ITEM_31, missingSourcePostId, null, null, null, 0))));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("첨부 1개가 교환 불가인데 생략하면 ITEM_NOT_AVAILABLE이고 저장하지 않는다")
    void handle_singleItemNotAvailableOmitted_throwsItemNotAvailable() {
        exchangeRepository.preload(postItemExchange(ExchangeStatus.ACTIVE,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0))));
        postPort.register(post(SOURCE_POST_31, "우사기 미니 피규어", REQUESTER_ID, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, null)));
    }

    @Test
    @DisplayName("첨부 2개가 모두 교환 불가인데 생략하면 ITEM_NOT_AVAILABLE이고 저장하지 않는다")
    void handle_allItemsNotAvailableOmitted_throwsItemNotAvailable() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(SOURCE_POST_31, "우사기 미니 피규어", REQUESTER_ID, PostInfo.Status.DELETED));
        postPort.register(post(SOURCE_POST_32, "하치와레 키링", REQUESTER_ID, PostInfo.Status.COMPLETED));

        assertRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, null)));
    }

    // ---- 시각 ----

    @Test
    @DisplayName("SCHEDULED인데 시각이 없으면 INVALID_SCHEDULED_AT이고 저장하지 않는다")
    void handle_scheduledWithoutTime_throwsInvalidScheduledAt() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_SCHEDULED_AT, () -> handler.handle(
                command(REQUESTER_ID, AppointmentTimeType.SCHEDULED, null, List.of(ITEM_31))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-26T05:00:00Z", "2026-10-26T04:50:00Z"})
    @DisplayName("시각이 지금이거나 그 이전이면 INVALID_SCHEDULED_AT이고 저장하지 않는다")
    void handle_pastTime_throwsInvalidScheduledAt(String scheduledAt) {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_SCHEDULED_AT, () -> handler.handle(
                command(REQUESTER_ID, AppointmentTimeType.SCHEDULED, Instant.parse(scheduledAt), List.of(ITEM_31))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-27T06:05:00Z", "2026-10-27T06:00:30Z"})
    @DisplayName("10분 단위가 아니면 INVALID_SCHEDULED_AT이고 저장하지 않는다")
    void handle_notTenMinuteStep_throwsInvalidScheduledAt(String scheduledAt) {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_SCHEDULED_AT, () -> handler.handle(
                command(REQUESTER_ID, AppointmentTimeType.SCHEDULED, Instant.parse(scheduledAt), List.of(ITEM_31))));
    }

    @Test
    @DisplayName("8일 후(11-03 00:00)면 INVALID_SCHEDULED_AT이고 저장하지 않는다")
    void handle_eighthDay_throwsInvalidScheduledAt() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.INVALID_SCHEDULED_AT, () -> handler.handle(command(REQUESTER_ID,
                AppointmentTimeType.SCHEDULED, Instant.parse("2026-11-02T15:00:00Z"), List.of(ITEM_31))));
    }

    // ---- 진행 중 약속 ----

    @Test
    @DisplayName("상대가 낸 만료 전 제안이 있으면 APPOINTMENT_ALREADY_EXISTS이고 저장하지 않는다")
    void handle_counterpartPendingProposal_throwsAlreadyExists() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.PROPOSED,
                NOW.plus(Duration.ofHours(1))));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("내가 낸 만료 전 제안이 있으면 APPOINTMENT_ALREADY_EXISTS이고 저장하지 않는다")
    void handle_ownPendingProposal_throwsAlreadyExists() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.PROPOSED,
                NOW.plus(Duration.ofMinutes(1))));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 200L})
    @DisplayName("확정 약속이 있으면 요청자 · 게시자 모두 APPOINTMENT_ALREADY_EXISTS이고 저장하지 않는다")
    void handle_confirmedAppointment_throwsAlreadyExists(long proposerId) {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED,
                null));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(proposerId, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("제안 후 25시간이 지나 옛 만료 시각이 지난 PROPOSED가 있으면 APPOINTMENT_ALREADY_EXISTS이고 저장하지 않는다")
    void handle_pastOldExpiryProposal_throwsAlreadyExists() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        Appointment stale = Appointment.of(501L, EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, Instant.parse("2026-10-28T06:00:00Z")),
                PLACE, new AppointmentItemIds(List.of(ITEM_31)), NOW.minus(Duration.ofHours(1)), 30, null, null,
                NOW.minus(Duration.ofHours(25)), NOW.minus(Duration.ofHours(25)));
        appointmentRepository.preload(stale);

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
        assertEquals(AppointmentStatus.PROPOSED, appointmentRepository.findById(501L).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("옛 만료 시각이 정확히 지금인 PROPOSED가 있으면 APPOINTMENT_ALREADY_EXISTS")
    void handle_proposalAtOldExpiry_throwsAlreadyExists() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.PROPOSED, NOW));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("만료 시각이 null인 PROPOSED가 있으면 APPOINTMENT_ALREADY_EXISTS")
    void handle_proposalWithoutExpiry_throwsAlreadyExists() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.PROPOSED, null));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(OWNER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("약속 시각이 지난 PROPOSED가 있어도 APPOINTMENT_ALREADY_EXISTS")
    void handle_proposalPastSchedule_throwsAlreadyExists() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(Appointment.of(501L, EXCHANGE_ID, OWNER_ID, REQUESTER_ID,
                AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, NOW.minus(Duration.ofHours(1))),
                PLACE, new AppointmentItemIds(List.of(ITEM_31)), null, 30, null, null,
                NOW.minus(Duration.ofHours(3)), NOW.minus(Duration.ofHours(3))));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("거절된 약속만 있으면 진행 중이 아니므로 새 제안이 저장된다")
    void handle_onlyRejectedAppointment_saves() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.REJECTED,
                NOW.plus(Duration.ofHours(1))));

        handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31)));

        assertEquals(1, appointmentRepository.saveCount());
    }

    @Test
    @DisplayName("같은 글의 다른 교환에 진행 중 제안이 있어도 이 교환의 제안은 저장된다")
    void handle_pendingProposalInOtherExchange_saves() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        exchangeRepository.preload(otherExchangeOnSamePost());
        appointmentRepository.preload(existing(OTHER_EXCHANGE_ID, OTHER_MEMBER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, NOW.plus(Duration.ofHours(1))));

        handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31)));

        assertEquals(1, appointmentRepository.saveCount());
    }

    // ---- 존재 · 권한 · 상태 ----

    @Test
    @DisplayName("없는 교환이면 EXCHANGE_NOT_FOUND이고 저장하지 않는다")
    void handle_missingExchange_throwsExchangeNotFound() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.EXCHANGE_NOT_FOUND, () -> handler.handle(new ProposeAppointmentCommand(
                MISSING_EXCHANGE_ID, REQUESTER_ID, AppointmentTimeType.SCHEDULED, SCHEDULED_AT, PLACE,
                List.of(ITEM_31))));
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 제안하면 NOT_EXCHANGE_PARTICIPANT이고 저장하지 않는다")
    void handle_thirdParty_throwsNotExchangeParticipant() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(scheduledCommand(OTHER_MEMBER_ID, List.of(ITEM_31))));
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("종료된(COMPLETED · CLOSED · LEFT) 교환이면 EXCHANGE_CLOSED이고 저장하지 않는다")
    void handle_notActiveExchange_throwsExchangeClosed(ExchangeStatus status) {
        exchangeRepository.preload(twoPostItemExchange(status));

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("요청받은 글이 예약중이면 POST_RESERVED_ELSEWHERE이고 저장하지 않는다")
    void handle_postReserved_throwsPostReservedElsewhere() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"COMPLETED", "DELETED"})
    @DisplayName("요청받은 글이 교환 완료 · 삭제면 POST_NOT_REQUESTABLE이고 저장하지 않는다")
    void handle_postCompletedOrDeleted_throwsPostNotRequestable(PostInfo.Status status) {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, status));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("요청받은 글이 조회되지 않으면 POST_NOT_REQUESTABLE이고 저장하지 않는다")
    void handle_postMissing_throwsPostNotRequestable() {
        Long missingPostId = 888L;
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, missingPostId, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    // ---- 판정 순서 (spec 가정 3) ----

    @Test
    @DisplayName("참여자 아님과 종료된 교환이 겹치면 NOT_EXCHANGE_PARTICIPANT가 먼저다")
    void handle_thirdPartyOnClosedExchange_participantFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.CLOSED));

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(scheduledCommand(OTHER_MEMBER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("종료된 교환과 지난 시각이 겹치면 EXCHANGE_CLOSED가 먼저다")
    void handle_closedExchangeWithPastTime_statusFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.CLOSED));

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED, () -> handler.handle(command(REQUESTER_ID,
                AppointmentTimeType.SCHEDULED, NOW.minus(Duration.ofHours(1)), List.of(ITEM_31))));
    }

    @Test
    @DisplayName("지난 시각과 진행 중 제안이 겹치면 INVALID_SCHEDULED_AT이 먼저다")
    void handle_pastTimeWithPendingProposal_timeFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.PROPOSED,
                NOW.plus(Duration.ofHours(1))));

        assertRejected(ExchangeErrorCode.INVALID_SCHEDULED_AT, () -> handler.handle(command(REQUESTER_ID,
                AppointmentTimeType.SCHEDULED, NOW.minus(Duration.ofHours(1)), List.of(ITEM_31))));
    }

    @Test
    @DisplayName("진행 중 제안과 글 예약중이 겹치면 APPOINTMENT_ALREADY_EXISTS가 먼저다")
    void handle_pendingProposalWithReservedPost_appointmentFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.PROPOSED,
                NOW.plus(Duration.ofHours(1))));
        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.APPOINTMENT_ALREADY_EXISTS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("글 예약중과 잘못된 아이템이 겹치면 POST_RESERVED_ELSEWHERE가 먼저다")
    void handle_reservedPostWithForeignItem_postFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(FOREIGN_ITEM))));
    }

    @Test
    @DisplayName("첨부가 아닌 ID와 교환 불가 아이템이 함께 있으면 INVALID_OFFERED_ITEMS가 먼저다")
    void handle_foreignAndUnavailableItems_invalidFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(SOURCE_POST_31, "우사기 미니 피규어", REQUESTER_ID, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.INVALID_OFFERED_ITEMS,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31, FOREIGN_ITEM))));
    }

    // ---- exchange-user-connect AC-11 차단 관계 ----

    @Test
    @DisplayName("요청자가 게시자를 차단했으면 요청자의 제안은 BLOCKED_USER이고 저장하지 않는다")
    void handle_requesterBlockedOwner_byRequester_throwsBlockedUser() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("게시자가 요청자를 차단했으면 요청자의 제안은 BLOCKED_USER이고 저장하지 않는다")
    void handle_ownerBlockedRequester_byRequester_throwsBlockedUser() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(OWNER_ID, REQUESTER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("요청자가 게시자를 차단했으면 게시자의 제안도 BLOCKED_USER이고 저장하지 않는다")
    void handle_requesterBlockedOwner_byOwner_throwsBlockedUser() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(OWNER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("게시자가 요청자를 차단했으면 게시자의 지금 바로 제안도 BLOCKED_USER이고 저장하지 않는다")
    void handle_ownerBlockedRequester_byOwnerNow_throwsBlockedUser() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(OWNER_ID, REQUESTER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER, () -> handler.handle(command(OWNER_ID,
                AppointmentTimeType.NOW, null, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("차단 판정은 제안자와 받는 사람으로 묻고 차단이 없으면 저장된다")
    void handle_noBlock_checksProposerAndReceiverThenSaves() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(REQUESTER_ID, OTHER_MEMBER_ID);

        Long appointmentId = handler.handle(scheduledCommand(OWNER_ID, List.of(ITEM_31)));

        assertNotNull(appointmentId);
        assertEquals(1, appointmentRepository.saveCount());
        assertEquals(List.of(List.of(OWNER_ID, REQUESTER_ID)), userPort.blockChecks());
    }

    // ---- exchange-user-connect AC-12 판정 순서 002 · 010 → 008 → 나머지 ----

    @Test
    @DisplayName("참여자가 아니면 차단 관계여도 NOT_EXCHANGE_PARTICIPANT가 먼저다")
    void handle_thirdPartyWithBlock_participantFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(OTHER_MEMBER_ID, OWNER_ID);
        userPort.block(OTHER_MEMBER_ID, REQUESTER_ID);

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT,
                () -> handler.handle(scheduledCommand(OTHER_MEMBER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("종료된 교환이면 차단 관계여도 EXCHANGE_CLOSED가 먼저다")
    void handle_closedExchangeWithBlock_closedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.CLOSED));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("차단 관계와 지난 약속 시각이 겹치면 BLOCKED_USER가 먼저다")
    void handle_blockWithPastTime_blockedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(OWNER_ID, REQUESTER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER, () -> handler.handle(command(REQUESTER_ID,
                AppointmentTimeType.SCHEDULED, NOW.minus(Duration.ofHours(1)), List.of(ITEM_31))));
    }

    @Test
    @DisplayName("차단 관계와 진행 중 제안이 겹치면 BLOCKED_USER가 먼저다")
    void handle_blockWithPendingProposal_blockedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(existing(EXCHANGE_ID, OWNER_ID, REQUESTER_ID, AppointmentStatus.PROPOSED,
                NOW.plus(Duration.ofHours(1))));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("차단 관계와 글 예약중이 겹치면 BLOCKED_USER가 먼저다")
    void handle_blockWithReservedPost_blockedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, PostInfo.Status.RESERVED));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("차단 관계와 글 교환 완료가 겹치면 BLOCKED_USER가 먼저다")
    void handle_blockWithCompletedPost_blockedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(POST_ID, "치이카와 교환해요", OWNER_ID, PostInfo.Status.COMPLETED));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    @Test
    @DisplayName("차단 관계와 첨부가 아닌 아이템이 겹치면 BLOCKED_USER가 먼저다")
    void handle_blockWithForeignItem_blockedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(FOREIGN_ITEM))));
    }

    @Test
    @DisplayName("차단 관계와 교환 불가 아이템이 겹치면 BLOCKED_USER가 먼저다")
    void handle_blockWithUnavailableItem_blockedFirst() {
        exchangeRepository.preload(twoPostItemExchange(ExchangeStatus.ACTIVE));
        postPort.register(post(SOURCE_POST_31, "우사기 미니 피규어", REQUESTER_ID, PostInfo.Status.RESERVED));
        userPort.block(REQUESTER_ID, OWNER_ID);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(scheduledCommand(REQUESTER_ID, List.of(ITEM_31))));
    }

    // ---- helpers ----

    private void assertRejected(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
        assertEquals(0, appointmentRepository.saveCount());
    }

    private static ProposeAppointmentCommand scheduledCommand(Long proposerId, List<Long> itemIds) {
        return command(proposerId, AppointmentTimeType.SCHEDULED, SCHEDULED_AT, itemIds);
    }

    private static ProposeAppointmentCommand command(Long proposerId, AppointmentTimeType timeType,
                                                     Instant scheduledAt, List<Long> itemIds) {
        return new ProposeAppointmentCommand(EXCHANGE_ID, proposerId, timeType, scheduledAt, PLACE, itemIds);
    }

    private static Exchange twoPostItemExchange(ExchangeStatus status) {
        return postItemExchange(status, List.of(
                OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0),
                OfferedItem.of(ITEM_32, SOURCE_POST_32, null, null, null, 1)));
    }

    private static Exchange postItemExchange(ExchangeStatus status, List<OfferedItem> items) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST, items,
                status, completedAt, NOW, NOW);
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_31, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                status, null, NOW, NOW);
    }

    private static Exchange otherExchangeOnSamePost() {
        return Exchange.of(OTHER_EXCHANGE_ID, POST_ID, OTHER_MEMBER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(41L, 601L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }

    private static Appointment existing(Long exchangeId, Long proposerId, Long receiverId,
                                        AppointmentStatus status, Instant expiresAt) {
        Instant createdAt = NOW.minus(Duration.ofHours(3));
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? NOW.minus(Duration.ofHours(2)) : null;
        return Appointment.of(500L + exchangeId, exchangeId, proposerId, receiverId, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, Instant.parse("2026-10-28T06:00:00Z")),
                PLACE, new AppointmentItemIds(List.of(ITEM_31)), expiresAt, null, null, confirmedAt,
                createdAt, createdAt);
    }

    private static PostInfo post(Long id, String title, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, title, thumbnailOf(id), ownerId, status);
    }

    private static String thumbnailOf(Long postId) {
        return "https://cdn.example.com/post/" + postId + "/1.jpg";
    }
}
