package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentButton;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E-2 교환 상세의 {@code appointmentButton}이 이 교환의 확정 약속 · 교환 상태 · 요청받은 게시글 상태로 정해지는지 확인한다 (#120).
 */
class GetExchangeHandlerAppointmentButtonTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-24T03:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-24T04:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-24T05:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-25T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-25T06:00:00Z");
    /** 교환 확인 열림 시각 T = 약속 시각 + 30분 */
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    private static final Instant RESOLVED_AT = SCHEDULED_AT.plus(Duration.ofHours(1));
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_REQUESTER_ID = 300L;
    private static final Long ITEM_ID = 31L;
    private static final Long OTHER_ITEM_ID = 41L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, CREATED_AT);
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        userPort.register(new UserInfo(REQUESTER_ID, "바꾸", 0));
        userPort.register(new UserInfo(OWNER_ID, "하치", 0));
    }

    // ---- AC-5 ----

    @Test
    @DisplayName("약속이 없고 게시글이 AVAILABLE이면 PROPOSE")
    void handle_noAppointment_returnsPropose() {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.AVAILABLE);

        assertEquals(AppointmentButton.PROPOSE, buttonAt(NOW, REQUESTER_ID));
    }

    @Test
    @DisplayName("진행 중 제안(PROPOSED)만 있으면 PROPOSE")
    void handle_pendingProposalOnly_returnsPropose() {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.AVAILABLE);
        appointmentRepository.preload(appointment(500L, EXCHANGE_ID, AppointmentStatus.PROPOSED));

        assertEquals(AppointmentButton.PROPOSE, buttonAt(NOW, OWNER_ID));
    }

    // ---- AC-6 ----

    @Test
    @DisplayName("이 교환에 확정 약속이 있으면 교환 확인 열림 1초 전 E-2에서 CANCEL")
    void handle_confirmedOneSecondBeforeOpen_returnsCancel() {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.RESERVED);
        appointmentRepository.preload(appointment(500L, EXCHANGE_ID, AppointmentStatus.CONFIRMED));

        assertEquals(AppointmentButton.CANCEL, buttonAt(OPENS_AT.minusSeconds(1), REQUESTER_ID));
    }

    @Test
    @DisplayName("지난 취소 약속과 새 확정 약속이 함께 있으면 확정 약속 기준으로 CANCEL")
    void handle_canceledAndConfirmed_usesConfirmed() {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.RESERVED);
        appointmentRepository.preload(appointment(499L, EXCHANGE_ID, AppointmentStatus.CANCELED));
        appointmentRepository.preload(appointment(500L, EXCHANGE_ID, AppointmentStatus.CONFIRMED));

        assertEquals(AppointmentButton.CANCEL, buttonAt(NOW, REQUESTER_ID));
    }

    // ---- AC-7 ----

    @ParameterizedTest
    @ValueSource(longs = {0L, 86_400L})
    @DisplayName("아무도 답하지 않은 확정 약속이면 교환 확인 열림 시각과 하루 뒤 E-2에서 NONE")
    void handle_confirmedAtOrAfterOpen_returnsNone(long secondsAfterOpen) {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.RESERVED);
        appointmentRepository.preload(appointment(500L, EXCHANGE_ID, AppointmentStatus.CONFIRMED));

        assertEquals(AppointmentButton.NONE, buttonAt(OPENS_AT.plusSeconds(secondsAfterOpen), REQUESTER_ID));
    }

    // ---- AC-8 ----

    @Test
    @DisplayName("교환 확인으로 완료(COMPLETED)된 교환이면 NONE")
    void handle_completedExchange_returnsNone() {
        givenExchange(ExchangeStatus.COMPLETED, PostInfo.Status.COMPLETED);
        appointmentRepository.preload(appointment(500L, EXCHANGE_ID, AppointmentStatus.COMPLETED));

        assertEquals(AppointmentButton.NONE, buttonAt(RESOLVED_AT, REQUESTER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"CLOSED", "LEFT"})
    @DisplayName("CLOSED · LEFT 교환이면 게시글이 AVAILABLE이어도 NONE")
    void handle_closedOrLeftExchange_returnsNone(ExchangeStatus status) {
        givenExchange(status, PostInfo.Status.AVAILABLE);

        assertEquals(AppointmentButton.NONE, buttonAt(NOW, OWNER_ID));
    }

    // ---- AC-9 ----

    @Test
    @DisplayName("같은 게시글이 다른 교환의 확정 약속으로 RESERVED면 PROPOSE_DISABLED")
    void handle_postReservedByOtherExchange_returnsProposeDisabled() {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.RESERVED);
        exchangeRepository.preload(newItemExchange(OTHER_EXCHANGE_ID, OTHER_REQUESTER_ID, OTHER_ITEM_ID,
                ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(600L, OTHER_EXCHANGE_ID, AppointmentStatus.CONFIRMED));

        assertEquals(AppointmentButton.PROPOSE_DISABLED, buttonAt(NOW, REQUESTER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"COMPLETED", "DELETED"})
    @DisplayName("게시글이 다른 교환과 완료됐거나 삭제됐으면 PROPOSE_DISABLED")
    void handle_postCompletedOrDeleted_returnsProposeDisabled(PostInfo.Status postStatus) {
        givenExchange(ExchangeStatus.ACTIVE, postStatus);

        assertEquals(AppointmentButton.PROPOSE_DISABLED, buttonAt(NOW, REQUESTER_ID));
    }

    @Test
    @DisplayName("게시글을 찾을 수 없으면 PROPOSE_DISABLED")
    void handle_missingPost_returnsProposeDisabled() {
        exchangeRepository.preload(newItemExchange(EXCHANGE_ID, REQUESTER_ID, ITEM_ID, ExchangeStatus.ACTIVE));

        assertEquals(AppointmentButton.PROPOSE_DISABLED, buttonAt(NOW, REQUESTER_ID));
    }

    // ---- AC-10 ----

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELED", "NOT_MET"})
    @DisplayName("확정 약속이 취소됐거나 못 만남(NOT_MET)으로 정해지고 게시글이 AVAILABLE이면 PROPOSE")
    void handle_afterCanceledOrNotMet_returnsPropose(AppointmentStatus status) {
        givenExchange(ExchangeStatus.ACTIVE, PostInfo.Status.AVAILABLE);
        appointmentRepository.preload(appointment(500L, EXCHANGE_ID, status));

        assertEquals(AppointmentButton.PROPOSE, buttonAt(RESOLVED_AT, REQUESTER_ID));
    }

    // ---- AC-11 ----

    @ParameterizedTest
    @MethodSource("sharedScenarios")
    @DisplayName("요청자와 게시자가 각각 E-2로 보면 같은 버튼이다")
    void handle_requesterAndOwner_seeSameButton(PostInfo.Status postStatus, AppointmentStatus appointmentStatus,
                                                AppointmentButton expected) {
        givenExchange(ExchangeStatus.ACTIVE, postStatus);
        if (appointmentStatus != null) {
            appointmentRepository.preload(appointment(500L, EXCHANGE_ID, appointmentStatus));
        }

        AppointmentButton requesterButton = buttonAt(NOW, REQUESTER_ID);
        AppointmentButton ownerButton = buttonAt(NOW, OWNER_ID);

        assertEquals(expected, requesterButton);
        assertEquals(expected, ownerButton);
    }

    /** AC-5 · AC-6 · AC-9 상황 */
    static Stream<Arguments> sharedScenarios() {
        return Stream.of(
                Arguments.of(PostInfo.Status.AVAILABLE, null, AppointmentButton.PROPOSE),
                Arguments.of(PostInfo.Status.RESERVED, AppointmentStatus.CONFIRMED, AppointmentButton.CANCEL),
                Arguments.of(PostInfo.Status.RESERVED, null, AppointmentButton.PROPOSE_DISABLED));
    }

    private AppointmentButton buttonAt(Instant now, Long viewerId) {
        GetExchangeHandler handler = new GetExchangeHandler(exchangeRepository, appointmentRepository, postPort,
                userPort, new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul"))));
        return handler.handle(new GetExchangeQuery(EXCHANGE_ID, viewerId)).appointmentButton();
    }

    private void givenExchange(ExchangeStatus status, PostInfo.Status postStatus) {
        exchangeRepository.preload(newItemExchange(EXCHANGE_ID, REQUESTER_ID, ITEM_ID, status));
        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, postStatus));
    }

    private static Exchange newItemExchange(Long exchangeId, Long requesterId, Long itemId, ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? RESOLVED_AT : null;
        return Exchange.of(exchangeId, POST_ID, requesterId, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(itemId, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    /** 일정 약속. 상태에 맞춰 만료 · 확정 · 결과 시각을 채운다. 제안자는 그 교환의 요청자다. */
    private Appointment appointment(Long id, Long exchangeId, AppointmentStatus status) {
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow();
        boolean pending = status == AppointmentStatus.PROPOSED;
        boolean wasConfirmed = status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.CANCELED
                || status == AppointmentStatus.NOT_MET || status == AppointmentStatus.COMPLETED;
        Instant resolvedAt = pending || status == AppointmentStatus.CONFIRMED ? null : RESOLVED_AT;
        return Appointment.of(id, exchangeId, exchange.getRequestUserId(), OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(exchange.getOfferedItems().get(0).getId())),
                EXPIRES_AT, 30, 30, wasConfirmed ? CONFIRMED_AT : null, resolvedAt, List.of(),
                CREATED_AT, CREATED_AT);
    }
}
