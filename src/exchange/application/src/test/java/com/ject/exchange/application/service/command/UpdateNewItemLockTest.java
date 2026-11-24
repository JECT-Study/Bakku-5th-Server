package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.dto.dto.NewItem;
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
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.PostLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E-3 새로 작성한 제품 수정이 이 교환의 확정 약속이 있으면 EXCHANGE_014로 막히는지,
 * 판정 순서와 A-4 수락과 겹칠 때의 글 잠금 순서를 확인한다 (#120).
 */
class UpdateNewItemLockTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-24T03:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-24T04:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-24T05:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plus(Duration.ofDays(1));
    private static final Instant SCHEDULED_AT = NOW.plus(Duration.ofDays(2));
    private static final Instant RESOLVED_AT = NOW.minus(Duration.ofMinutes(10));
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long SOURCE_POST_ID = 501L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_ID = 31L;
    private static final Long OTHER_ITEM_ID = 41L;
    private static final Long APPOINTMENT_ID = 500L;
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String ORIGINAL_DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";
    private static final String ORIGINAL_IMAGE_KEY = "exchange/1/0b1c2d3e-0001.jpg";
    private static final String NEW_IMAGE_KEY = "exchange/tmp/100/0b1c2d3e-0002.jpg";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakeImagePort imagePort;
    private UpdateNewItemHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        imagePort = new FakeImagePort();
        handler = new UpdateNewItemHandler(exchangeRepository, appointmentRepository, postLockRepository, imagePort);
    }

    // ---- AC-1 ----

    @Test
    @DisplayName("이 교환의 일정 약속이 CONFIRMED면 요청자 수정은 NEW_ITEM_LOCKED이고 저장 · 사진 확정이 없다")
    void handle_scheduledConfirmed_throwsNewItemLocked() {
        // given
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT)));

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(REQUESTER_ID)));

        // then
        assertEquals(ExchangeErrorCode.NEW_ITEM_LOCKED, exception.getErrorCode());
        assertNothingChanged();
    }

    @Test
    @DisplayName("지금 바로 약속이 CONFIRMED여도 요청자 수정은 NEW_ITEM_LOCKED이고 저장 · 사진 확정이 없다")
    void handle_nowTypeConfirmed_throwsNewItemLocked() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.NOW, null)));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.NEW_ITEM_LOCKED, exception.getErrorCode());
        assertNothingChanged();
    }

    // ---- AC-2 ----

    @Test
    @DisplayName("확정 약속이 있어도 게시자가 수정하면 NEW_ITEM_LOCKED가 아니라 NOT_REQUESTER")
    void handle_confirmedByOwner_throwsNotRequesterFirst() {
        givenConfirmedOn(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(OWNER_ID)));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
        assertNothingChanged();
    }

    @Test
    @DisplayName("확정 약속이 있어도 참여자가 아닌 회원이 수정하면 NOT_REQUESTER")
    void handle_confirmedByThirdParty_throwsNotRequesterFirst() {
        givenConfirmedOn(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
        assertNothingChanged();
    }

    @Test
    @DisplayName("확정 약속이 있는 POST 방식 교환이면 NEW_ITEM_LOCKED가 아니라 NOT_NEW_ITEM_OFFER")
    void handle_confirmedPostOffer_throwsNotNewItemOfferFirst() {
        givenConfirmedOn(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, SOURCE_POST_ID, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.NOT_NEW_ITEM_OFFER, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertTrue(imagePort.calls().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("확정 약속이 있는 교환이 ACTIVE가 아니면 NEW_ITEM_LOCKED가 아니라 EXCHANGE_CLOSED")
    void handle_confirmedNotActive_throwsExchangeClosedFirst(ExchangeStatus status) {
        givenConfirmedOn(newItemExchange(status));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.EXCHANGE_CLOSED, exception.getErrorCode());
        assertNothingChanged();
    }

    // ---- AC-3 ----

    @Test
    @DisplayName("이 교환에 약속이 없으면 요청자 수정이 저장된다")
    void handle_noAppointment_saves() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        Long result = handler.handle(validCommand(REQUESTER_ID));

        assertEquals(EXCHANGE_ID, result);
        assertUpdated();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
            names = {"PROPOSED", "REJECTED", "WITHDRAWN", "AUTO_REJECTED", "CANCELED", "NOT_MET"})
    @DisplayName("이 교환의 약속이 확정(CONFIRMED)이 아니면 요청자 수정이 저장된다")
    void handle_nonConfirmedAppointment_saves(AppointmentStatus status) {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT)));

        Long result = handler.handle(validCommand(REQUESTER_ID));

        assertEquals(EXCHANGE_ID, result);
        assertUpdated();
    }

    @Test
    @DisplayName("같은 게시글의 다른 교환에만 확정 약속이 있으면 이 교환의 수정은 저장된다")
    void handle_confirmedOnlyInOtherExchange_saves() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        exchangeRepository.preload(Exchange.of(OTHER_EXCHANGE_ID, POST_ID, OTHER_MEMBER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(OTHER_ITEM_ID, null, new ItemName("하치와레 키링"), true, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(appointment(600L, OTHER_EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT)));

        handler.handle(validCommand(REQUESTER_ID));

        assertUpdated();
    }

    // ---- AC-4 ----

    @Test
    @DisplayName("E-3는 A-4 · F-1과 같은 교환 게시글 잠금을 잡는다")
    void handle_locksExchangePost() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        handler.handle(validCommand(REQUESTER_ID));

        assertEquals(List.of(List.of(POST_ID)), postLockRepository.lockedPostIds());
    }

    @Test
    @DisplayName("E-3가 글 잠금을 기다리는 동안 A-4 수락이 먼저 확정하면 E-3는 NEW_ITEM_LOCKED이고 저장되지 않는다")
    void handle_acceptCommittedWhileWaitingForLock_throwsNewItemLocked() {
        // given: 처음 읽을 때는 PROPOSED 제안뿐이고, 글 잠금을 얻는 순간 A-4가 이미 확정해 둔 상태가 된다
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT)));
        ConfirmOnLockPostLockRepository lockAfterAccept = new ConfirmOnLockPostLockRepository(
                () -> appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID,
                        AppointmentStatus.CONFIRMED, new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT))));
        UpdateNewItemHandler racingHandler = new UpdateNewItemHandler(exchangeRepository, appointmentRepository,
                lockAfterAccept, imagePort);

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> racingHandler.handle(validCommand(REQUESTER_ID)));

        // then
        assertEquals(ExchangeErrorCode.NEW_ITEM_LOCKED, exception.getErrorCode());
        assertEquals(List.of(List.of(POST_ID)), lockAfterAccept.lockedPostIds());
        assertNothingChanged();
    }

    @Test
    @DisplayName("E-3가 글 잠금을 기다리는 동안 F-1이 교환을 완료하면 E-3는 EXCHANGE_CLOSED이고 완료 상태를 되돌리지 않는다")
    void handle_completeCommittedWhileWaitingForLock_keepsCompletedExchange() {
        // given: 처음에는 ACTIVE 교환이고, 글 잠금을 얻는 순간 F-1이 이미 완료해 둔 상태가 된다
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        ConfirmOnLockPostLockRepository lockAfterComplete = new ConfirmOnLockPostLockRepository(
                () -> exchangeRepository.preload(newItemExchange(ExchangeStatus.COMPLETED)));
        UpdateNewItemHandler racingHandler = new UpdateNewItemHandler(exchangeRepository, appointmentRepository,
                lockAfterComplete, imagePort);

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> racingHandler.handle(validCommand(REQUESTER_ID)));

        // then: 잠금 전에 읽은 낡은 ACTIVE 교환을 저장해 완료를 덮어쓰지 않는다
        assertEquals(ExchangeErrorCode.EXCHANGE_CLOSED, exception.getErrorCode());
        assertEquals(List.of(List.of(POST_ID)), lockAfterComplete.lockedPostIds());
        assertEquals(ExchangeStatus.COMPLETED,
                exchangeRepository.findById(EXCHANGE_ID).orElseThrow().getStatus());
        assertNothingChanged();
    }

    @Test
    @DisplayName("교환이 없으면 글 잠금 없이 EXCHANGE_NOT_FOUND")
    void handle_missingExchange_throwsNotFoundWithoutLock() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(validCommand(REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.EXCHANGE_NOT_FOUND, exception.getErrorCode());
        assertTrue(postLockRepository.lockedPostIds().isEmpty());
        assertEquals(0, exchangeRepository.saveCount());
        assertTrue(imagePort.calls().isEmpty());
    }

    @Test
    @DisplayName("E-3가 먼저 처리되면 A-4는 수정된 제품으로 확정하고 그 뒤 E-3는 NEW_ITEM_LOCKED이다")
    void handle_updateThenAccept_confirmsUpdatedItemAndLocksFurtherUpdates() {
        // given
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT)));
        FakePostPort postPort = new FakePostPort();
        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        AcceptAppointmentHandler acceptHandler = new AcceptAppointmentHandler(appointmentRepository,
                exchangeRepository, postLockRepository, postPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        // when: E-3 → A-4 순서
        handler.handle(validCommand(REQUESTER_ID));
        acceptHandler.handle(new AcceptAppointmentCommand(APPOINTMENT_ID, OWNER_ID));

        // then: 수정된 제품으로 확정됐다
        assertEquals(AppointmentStatus.CONFIRMED,
                appointmentRepository.findById(APPOINTMENT_ID).orElseThrow().getStatus());
        assertEquals(List.of(ITEM_ID),
                appointmentRepository.findById(APPOINTMENT_ID).orElseThrow().getItemIds().values());
        assertEquals("새 이름", storedItem().getName().value());

        // then: 확정 뒤 E-3는 막히고 수정된 값이 그대로 남는다
        int savesBefore = exchangeRepository.saveCount();
        ExchangeException exception = assertThrows(ExchangeException.class, () -> handler.handle(
                new UpdateNewItemCommand(EXCHANGE_ID, REQUESTER_ID,
                        new NewItem("확정 뒤 이름", false, null, List.of(NEW_IMAGE_KEY)))));
        assertEquals(ExchangeErrorCode.NEW_ITEM_LOCKED, exception.getErrorCode());
        assertEquals(savesBefore, exchangeRepository.saveCount());
        assertEquals("새 이름", storedItem().getName().value());
    }

    private void givenConfirmedOn(Exchange exchange) {
        exchangeRepository.preload(exchange);
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT)));
    }

    private static UpdateNewItemCommand validCommand(Long memberId) {
        return new UpdateNewItemCommand(EXCHANGE_ID, memberId,
                new NewItem("새 이름", true, "새 설명", List.of(NEW_IMAGE_KEY)));
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? RESOLVED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription(ORIGINAL_DESCRIPTION), 0, List.of(ORIGINAL_IMAGE_KEY))),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    private static Appointment appointment(Long id, Long exchangeId, AppointmentStatus status, AppointmentTime time) {
        boolean pending = status == AppointmentStatus.PROPOSED;
        boolean wasConfirmed = status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.CANCELED
                || status == AppointmentStatus.NOT_MET;
        Instant resolvedAt = pending || status == AppointmentStatus.CONFIRMED ? null : RESOLVED_AT;
        Long proposerId = exchangeId.equals(EXCHANGE_ID) ? REQUESTER_ID : OTHER_MEMBER_ID;
        Long itemId = exchangeId.equals(EXCHANGE_ID) ? ITEM_ID : OTHER_ITEM_ID;
        return Appointment.of(id, exchangeId, proposerId, OWNER_ID, status, time, PLACE,
                new AppointmentItemIds(List.of(itemId)), EXPIRES_AT, 30, null,
                wasConfirmed ? CONFIRMED_AT : null, resolvedAt, List.of(), CREATED_AT, CREATED_AT);
    }

    private OfferedItem storedItem() {
        Exchange stored = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(1, stored.getOfferedItems().size());
        return stored.getOfferedItems().get(0);
    }

    private void assertUpdated() {
        OfferedItem item = storedItem();
        assertEquals(ITEM_ID, item.getId());
        assertEquals("새 이름", item.getName().value());
        assertEquals(true, item.getOpened());
        assertEquals("새 설명", item.getDescription().value());
        assertEquals(1, imagePort.calls().size());
    }

    /** 제품명 · 상태 · 설명 · 사진이 그대로이고, 저장과 사진 확정(image BC) 요청이 없다. */
    private void assertNothingChanged() {
        assertEquals(0, exchangeRepository.saveCount());
        assertTrue(imagePort.calls().isEmpty());
        OfferedItem item = storedItem();
        assertEquals(ORIGINAL_NAME, item.getName().value());
        assertEquals(false, item.getOpened());
        assertEquals(ORIGINAL_DESCRIPTION, item.getDescription().value());
        assertEquals(List.of(ORIGINAL_IMAGE_KEY), item.getImageKeys());
    }

    /**
     * 글 잠금을 얻는 순간 다른 트랜잭션(A-4)의 커밋이 보이게 되는 상황을 흉내 낸다.
     * 잠금 뒤에 약속을 읽는 구현만 이 커밋을 본다.
     */
    private static final class ConfirmOnLockPostLockRepository implements PostLockRepository {

        private final Runnable commitWhileWaiting;
        private final List<List<Long>> lockedPostIds = new ArrayList<>();

        private ConfirmOnLockPostLockRepository(Runnable commitWhileWaiting) {
            this.commitWhileWaiting = commitWhileWaiting;
        }

        @Override
        public void lockAll(Collection<Long> postIds) {
            lockedPostIds.add(List.copyOf(postIds));
            commitWhileWaiting.run();
        }

        List<List<Long>> lockedPostIds() {
            return lockedPostIds;
        }
    }
}
