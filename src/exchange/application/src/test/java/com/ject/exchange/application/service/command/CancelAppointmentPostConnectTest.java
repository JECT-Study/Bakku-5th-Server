package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.CancelAppointmentCommand;
import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.dto.query.GetCurrentAppointmentQuery;
import com.ject.exchange.application.port.PostStatusRejectedException;
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
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A-8 약속 취소가 post BC 글을 교환 가능으로 되돌리고, 그 뒤 같은 글에 다시 약속 · 요청을 받는지 확인한다 (#113).
 * 롤백 · 실제 동시성은 DB에서만 판정되므로 Fake로는 전파 여부 · 잠금 순서 · 순차 재요청만 본다.
 */
class CancelAppointmentPostConnectTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 10:00 (+09:00) 취소 시각 */
    private static final Instant NOW = Instant.parse("2026-11-23T01:00:00Z");
    /** 2026-11-24 15:00 (+09:00) 취소 뒤 다시 제안하는 시각 */
    private static final Instant NEXT_SCHEDULED_AT = Instant.parse("2026-11-24T06:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    /** A-B 교환 X */
    private static final Long EXCHANGE_X = 1L;
    /** C-B 교환 Y (같은 글 P) */
    private static final Long EXCHANGE_Y = 2L;
    /** B의 글 P */
    private static final Long POST_P = 10L;
    /** A의 글 Q1 · Q2 */
    private static final Long POST_Q1 = 20L;
    private static final Long POST_Q2 = 21L;
    /** C의 글 (Y의 첨부 글) */
    private static final Long POST_OF_C = 40L;
    /** D의 글 (새 요청의 첨부 글) */
    private static final Long POST_OF_D = 50L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;
    private static final Long MEMBER_D = 104L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final Long ITEM_OF_C = 41L;
    private static final Long ITEM_NEW = 61L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private ServiceTime serviceTime;
    private CancelAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new CancelAppointmentHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, serviceTime);

        // X의 확정 약속이 P · Q1을 예약중으로 만든 상태. Q2는 고르지 않아 교환 가능
        postPort.register(post(POST_P, MEMBER_B, PostInfo.Status.RESERVED));
        postPort.register(post(POST_Q1, MEMBER_A, PostInfo.Status.RESERVED));
        postPort.register(post(POST_Q2, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_OF_C, MEMBER_C, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_OF_D, MEMBER_D, PostInfo.Status.AVAILABLE));
        exchangeRepository.preload(exchangeX());
        appointmentRepository.preload(confirmed(EXCHANGE_X, List.of(ITEM_Q1)));
    }

    // ---- AC-3 글 교환 가능으로 ----

    @Test
    @DisplayName("POST 방식 약속을 취소하면 release가 P · Q1로 한 번 호출되고 Q2는 포함되지 않는다")
    void handle_postOffer_releasesPostAndChosenAttachedPost() {
        // when
        handler.handle(cancel(MEMBER_A));

        // then
        assertEquals(1, postPort.releaseRequests().size());
        List<Long> released = postPort.releaseRequests().get(0);
        assertEquals(Set.of(POST_P, POST_Q1), Set.copyOf(released));
        assertEquals(2, released.size());
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q1));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q2));
        assertTrue(postPort.completeRequests().isEmpty());
        assertTrue(postPort.reserveRequests().isEmpty());
    }

    @Test
    @DisplayName("NEW 방식 약속을 취소하면 release는 P만 받는다")
    void handle_newItemOffer_releasesOnlyPost() {
        // given
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_NEW, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(confirmed(EXCHANGE_X, List.of(ITEM_NEW)));

        // when
        handler.handle(cancel(MEMBER_B));

        // then
        assertEquals(List.of(List.of(POST_P)), postPort.releaseRequests());
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_P));
    }

    // ---- AC-10 취소 뒤 다시 약속 ----

    @Test
    @DisplayName("취소 전에는 Y의 A-1 제안과 D의 P에 대한 E-1 요청이 EXCHANGE_108이다")
    void beforeCancel_proposeInOtherExchangeAndNewRequest_throwPostReservedElsewhere() {
        // given
        exchangeRepository.preload(exchangeY());

        // when · then
        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> proposeHandler().handle(propose(EXCHANGE_Y, MEMBER_C, ITEM_OF_C)));
        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> createHandler().handle(requestOnPostP(MEMBER_D)));
    }

    @Test
    @DisplayName("취소 뒤에는 X와 Y에서 A-1 제안이 저장되고 D의 P에 대한 E-1 요청도 저장된다")
    void afterCancel_proposeAndNewRequest_succeed() {
        // given
        exchangeRepository.preload(exchangeY());
        handler.handle(cancel(MEMBER_A));
        ProposeAppointmentHandler proposeHandler = proposeHandler();

        // when
        Long proposalInX = proposeHandler.handle(propose(EXCHANGE_X, MEMBER_A, ITEM_Q1));
        Long proposalInY = proposeHandler.handle(propose(EXCHANGE_Y, MEMBER_C, ITEM_OF_C));
        Long exchangeOfD = createHandler().handle(requestOnPostP(MEMBER_D));

        // then
        assertEquals(AppointmentStatus.PROPOSED, appointmentRepository.findById(proposalInX).orElseThrow().getStatus());
        assertEquals(AppointmentStatus.PROPOSED, appointmentRepository.findById(proposalInY).orElseThrow().getStatus());
        Exchange created = exchangeRepository.findById(exchangeOfD).orElseThrow();
        assertEquals(POST_P, created.getPostId());
        assertEquals(MEMBER_D, created.getRequestUserId());
        assertEquals(ExchangeStatus.ACTIVE, exchangeRepository.findById(EXCHANGE_X).orElseThrow().getStatus());
    }

    // ---- AC-11 글 상태 변경 거부 ----

    @Test
    @DisplayName("post BC가 release를 거부하면 PostStatusRejectedException이 그대로 전파되고 취소 결과를 저장하지 않는다")
    void handle_releaseRejected_propagatesWithoutSaving() {
        // given
        postPort.rejectStatusChangeOf(POST_P);

        // when
        PostStatusRejectedException exception = assertThrows(PostStatusRejectedException.class,
                () -> handler.handle(cancel(MEMBER_A)));

        // then
        assertEquals(POST_P, exception.getPostId());
        assertEquals(1, postPort.releaseRequests().size());
        assertEquals(0, appointmentRepository.saveCount());
        assertEquals(AppointmentStatus.CONFIRMED, stored().getStatus());
        assertNull(stored().getCanceledByUserId());
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_Q1));
    }

    // ---- AC-12 동시 취소 ----

    @Test
    @DisplayName("글 P · Q1을 먼저 잠근 뒤 약속 행을 잠가 조회한다")
    void handle_locksPostsBeforeAppointmentRow() {
        handler.handle(cancel(MEMBER_A));

        assertEquals(1, postLockRepository.lockedPostIds().size());
        assertEquals(Set.of(POST_P, POST_Q1), Set.copyOf(postLockRepository.lockedPostIds().get(0)));
        assertEquals(List.of(0), postLockRepository.lockedLookupCountsAtLock());
        assertTrue(appointmentRepository.lockedLookupCount() >= 1);
    }

    @Test
    @DisplayName("A가 취소한 뒤 B가 이어서 취소하면 EXCHANGE_109이고 release는 한 번만 반영된다")
    void handle_secondCancel_throwsInvalidStatusAndReleasesOnce() {
        // given
        handler.handle(cancel(MEMBER_A));

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(cancel(MEMBER_B)));

        // then
        assertEquals(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, exception.getErrorCode());
        assertEquals(1, postPort.releaseRequests().size());
        assertEquals(1, appointmentRepository.saveCount());
        assertEquals(MEMBER_A, stored().getCanceledByUserId());
    }

    // ---- AC-13 취소된 약속 조회 ----

    @Test
    @DisplayName("취소 뒤 A-2 조회는 CANCELED · 취소한 사람 A를 반환한다")
    void getAppointment_afterCancel_returnsCanceledBy() {
        // given
        handler.handle(cancel(MEMBER_A));

        // when
        AppointmentResult byProposer = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                serviceTime).handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));
        AppointmentResult byReceiver = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                serviceTime).handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_B));

        // then
        assertEquals(AppointmentStatus.CANCELED, byProposer.status());
        assertEquals(MEMBER_A, byProposer.canceledByUserId());
        assertEquals(AppointmentStatus.CANCELED, byReceiver.status());
        assertEquals(MEMBER_A, byReceiver.canceledByUserId());
    }

    @Test
    @DisplayName("취소되지 않은 약속의 A-2 조회는 취소한 사람이 null이다")
    void getAppointment_notCanceled_hasNullCanceledBy() {
        AppointmentResult result = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                serviceTime).handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertNull(result.canceledByUserId());
    }

    @Test
    @DisplayName("취소 뒤 A-3 현재 유효한 약속은 없다")
    void getCurrentAppointment_afterCancel_returnsEmpty() {
        // given
        GetCurrentAppointmentHandler currentHandler = new GetCurrentAppointmentHandler(appointmentRepository,
                exchangeRepository, postPort, serviceTime);
        assertNotNull(currentHandler.handle(new GetCurrentAppointmentQuery(EXCHANGE_X, MEMBER_A)).orElse(null));

        // when
        handler.handle(cancel(MEMBER_A));

        // then
        assertTrue(currentHandler.handle(new GetCurrentAppointmentQuery(EXCHANGE_X, MEMBER_A)).isEmpty());
        assertTrue(currentHandler.handle(new GetCurrentAppointmentQuery(EXCHANGE_X, MEMBER_B)).isEmpty());
    }

    // ---- helpers ----

    private static void assertRejected(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    private Appointment stored() {
        return appointmentRepository.findById(APPOINTMENT_ID).orElseThrow();
    }

    private ProposeAppointmentHandler proposeHandler() {
        return new ProposeAppointmentHandler(exchangeRepository, appointmentRepository, postPort, userPort,
                serviceTime);
    }

    private CreateExchangeHandler createHandler() {
        return new CreateExchangeHandler(exchangeRepository, postPort, userPort, new FakeImagePort());
    }

    private static CancelAppointmentCommand cancel(Long cancelerId) {
        return new CancelAppointmentCommand(APPOINTMENT_ID, cancelerId);
    }

    private static ProposeAppointmentCommand propose(Long exchangeId, Long proposerId, Long itemId) {
        return new ProposeAppointmentCommand(exchangeId, proposerId, AppointmentTimeType.SCHEDULED,
                NEXT_SCHEDULED_AT, PLACE, List.of(itemId), 30);
    }

    /** D가 B의 글 P에 D의 글을 첨부해 교환을 요청한다 */
    private static CreateExchangeCommand requestOnPostP(Long requesterId) {
        return new CreateExchangeCommand(requesterId, POST_P, OfferType.POST, List.of(POST_OF_D), null);
    }

    /** A가 제안하고 B가 수락한 SCHEDULED 15:00 약속 */
    private static Appointment confirmed(Long exchangeId, List<Long> itemIds) {
        return Appointment.of(APPOINTMENT_ID, exchangeId, MEMBER_A, MEMBER_B, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(itemIds), EXPIRES_AT, 30, 60, CONFIRMED_AT, null, List.of(),
                CREATED_AT, CREATED_AT);
    }

    /** 교환 X: A가 B의 글 P에 Q1 · Q2를 첨부 */
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

    private static PostInfo post(Long id, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, "글 " + id, "post/" + id + "/1.jpg", ownerId, status);
    }
}
