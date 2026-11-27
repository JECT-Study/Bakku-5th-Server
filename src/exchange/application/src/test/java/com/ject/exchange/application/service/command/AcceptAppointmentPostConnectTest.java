package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A-4 약속 수락이 post BC 글 상태를 예약중으로 바꾸고, 거부를 exchange 에러로 바꾸는지 확인한다 (#107).
 */
class AcceptAppointmentPostConnectTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = NOW.minus(Duration.ofHours(1));
    /** A-B 교환 X */
    private static final Long EXCHANGE_X = 1L;
    /** C-B 교환 Y (같은 글 P) */
    private static final Long EXCHANGE_Y = 2L;
    /** X에서 Q1을 고른 제안 */
    private static final Long APPOINTMENT_ID = 3L;
    /** Y에서 C가 한 제안 */
    private static final Long OTHER_APPOINTMENT_ID = 12L;
    /** B의 글 P */
    private static final Long POST_P = 10L;
    /** A의 글 Q1 · Q2 */
    private static final Long POST_Q1 = 20L;
    private static final Long POST_Q2 = 21L;
    /** C의 글 (Y의 첨부 글) */
    private static final Long POST_OF_C = 40L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final Long ITEM_OF_C = 41L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private ServiceTime serviceTime;
    private AcceptAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new AcceptAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, serviceTime);

        postPort.register(post(POST_P, MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_Q1, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_Q2, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_OF_C, MEMBER_C, PostInfo.Status.AVAILABLE));
        exchangeRepository.preload(exchangeX());
        appointmentRepository.preload(proposal(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, List.of(ITEM_Q1)));
    }

    // ---- AC-9 ----

    @Test
    @DisplayName("B가 Q1을 고른 제안을 수락하면 확정되고 P · Q1이 예약중 · Q2는 교환 가능 그대로다")
    void handle_accept_reservesPostAndChosenAttachedPost() {
        // when
        Long acceptedId = handler.handle(accept(APPOINTMENT_ID));

        // then
        assertEquals(APPOINTMENT_ID, acceptedId);
        assertEquals(AppointmentStatus.CONFIRMED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(1, postPort.reserveRequests().size());
        assertEquals(Set.of(POST_P, POST_Q1), Set.copyOf(postPort.reserveRequests().get(0)));
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_Q1));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q2));
    }

    // ---- AC-10 ----

    @Test
    @DisplayName("post BC가 교환글 P의 예약중 전환을 거부하면 POST_NOT_REQUESTABLE이다")
    void handle_reserveRejectedForPost_throwsPostNotRequestable() {
        postPort.rejectStatusChangeOf(POST_P);

        assertReserveRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE);
    }

    @Test
    @DisplayName("post BC가 고른 첨부 글 Q1의 예약중 전환을 거부하면 ITEM_NOT_AVAILABLE이다")
    void handle_reserveRejectedForChosenAttachedPost_throwsItemNotAvailable() {
        postPort.rejectStatusChangeOf(POST_Q1);

        assertReserveRejected(ExchangeErrorCode.ITEM_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("예약중 전환이 거부되면 약속은 제안 상태 그대로이고 같은 글 다른 방 제안도 자동 거절되지 않는다")
    void handle_reserveRejected_keepsProposalsAndPosts() {
        // given: Y에서 C가 B에게 한 진행 중 제안
        exchangeRepository.preload(exchangeY());
        appointmentRepository.preload(proposal(OTHER_APPOINTMENT_ID, EXCHANGE_Y, MEMBER_C, List.of(ITEM_OF_C)));
        postPort.rejectStatusChangeOf(POST_Q1);

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(accept(APPOINTMENT_ID)));

        // then
        assertEquals(ExchangeErrorCode.ITEM_NOT_AVAILABLE, exception.getErrorCode());
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
        assertNull(stored(APPOINTMENT_ID).getConfirmedAt());
        assertEquals(AppointmentStatus.PROPOSED, stored(OTHER_APPOINTMENT_ID).getStatus());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q1));
    }

    // ---- AC-11 ----

    @Test
    @DisplayName("X 확정 뒤 P가 예약중이면 C의 같은 글 새 요청은 POST_RESERVED_ELSEWHERE다")
    void createAfterAccept_byOtherMember_throwsPostReservedElsewhere() {
        // given
        handler.handle(accept(APPOINTMENT_ID));
        CreateExchangeHandler createHandler = new CreateExchangeHandler(exchangeRepository, postPort, userPort,
                new FakeImagePort(), new FakeChatRoomPort());
        int savesBefore = exchangeRepository.saveCount();

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> createHandler.handle(new CreateExchangeCommand(MEMBER_C, POST_P, OfferType.NEW, null,
                        new NewItem("치이카와 아크릴 스탠드", false, "가챠에서 두 개 나와서 하나 교환해요", List.of("exchange/tmp/100/item-1.jpg")))));

        // then
        assertEquals(ExchangeErrorCode.POST_RESERVED_ELSEWHERE, exception.getErrorCode());
        assertEquals(savesBefore, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("X 확정 뒤 Y에서 C가 약속을 제안하면 POST_RESERVED_ELSEWHERE이고 저장하지 않는다")
    void proposeAfterAccept_inOtherExchange_throwsPostReservedElsewhere() {
        // given
        exchangeRepository.preload(exchangeY());
        handler.handle(accept(APPOINTMENT_ID));
        ProposeAppointmentHandler proposeHandler = new ProposeAppointmentHandler(exchangeRepository,
                appointmentRepository, postPort, userPort, serviceTime);
        int savesBefore = appointmentRepository.saveCount();

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> proposeHandler.handle(new ProposeAppointmentCommand(EXCHANGE_Y, MEMBER_C,
                        AppointmentTimeType.SCHEDULED, SCHEDULED_AT, PLACE, List.of(ITEM_OF_C))));

        // then
        assertEquals(ExchangeErrorCode.POST_RESERVED_ELSEWHERE, exception.getErrorCode());
        assertEquals(savesBefore, appointmentRepository.saveCount());
    }

    // ---- AC-12 ----

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"COMPLETED", "DELETED"})
    @DisplayName("P가 교환 완료 · 삭제면 수락은 POST_NOT_REQUESTABLE이고 예약중 요청이 가지 않는다")
    void handle_postCompletedOrDeleted_throwsPostNotRequestable(PostInfo.Status status) {
        postPort.register(post(POST_P, MEMBER_B, status));

        assertRejectedBeforeReserve(ExchangeErrorCode.POST_NOT_REQUESTABLE);
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"RESERVED", "COMPLETED", "DELETED"})
    @DisplayName("Q1이 예약중 · 교환 완료 · 삭제면 수락은 ITEM_NOT_AVAILABLE이고 약속은 제안 상태 그대로다")
    void handle_chosenAttachedPostUnavailable_throwsItemNotAvailable(PostInfo.Status status) {
        postPort.register(post(POST_Q1, MEMBER_A, status));

        assertRejectedBeforeReserve(ExchangeErrorCode.ITEM_NOT_AVAILABLE);
    }

    // ---- helpers ----

    /** 예약중 요청까지 갔다가 거부되면 지정한 에러이고, 약속 · 자동 거절이 저장되지 않으며 글 상태도 그대로다. */
    private void assertReserveRejected(ExchangeErrorCode expected) {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(accept(APPOINTMENT_ID)));

        assertEquals(expected, exception.getErrorCode());
        assertEquals(1, postPort.reserveRequests().size());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q1));
    }

    private void assertRejectedBeforeReserve(ExchangeErrorCode expected) {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(accept(APPOINTMENT_ID)));

        assertEquals(expected, exception.getErrorCode());
        assertTrue(postPort.reserveRequests().isEmpty());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(AppointmentStatus.PROPOSED, stored(APPOINTMENT_ID).getStatus());
    }

    private Appointment stored(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    private static AcceptAppointmentCommand accept(Long appointmentId) {
        return new AcceptAppointmentCommand(appointmentId, MEMBER_B);
    }

    /** 교환 X: A가 P에 Q1 · Q2를 첨부 */
    private static Exchange exchangeX() {
        return Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0),
                        OfferedItem.of(ITEM_Q2, POST_Q2, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);
    }

    /** 교환 Y: C가 같은 글 P에 C의 글을 첨부 */
    private static Exchange exchangeY() {
        return Exchange.of(EXCHANGE_Y, POST_P, MEMBER_C, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_OF_C, POST_OF_C, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);
    }

    private static Appointment proposal(Long id, Long exchangeId, Long proposerId, List<Long> itemIds) {
        return Appointment.of(id, exchangeId, proposerId, MEMBER_B, AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(itemIds), EXPIRES_AT, 30, null, null, CREATED_AT, CREATED_AT);
    }

    private static PostInfo post(Long id, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, "글 " + id, "post/" + id + "/1.jpg", ownerId, status);
    }
}
