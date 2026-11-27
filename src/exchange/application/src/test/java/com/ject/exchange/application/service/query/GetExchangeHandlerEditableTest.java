package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.OfferedItemResult;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E-2 교환 상세의 {@code offeredItems[].editable}이 조회자 · 첨부 방식 · 교환 상태 · 이 교환의 확정 약속으로 정해지는지 확인한다 (#120).
 */
class GetExchangeHandlerEditableTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-24T03:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-24T04:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-24T05:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-25T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-25T06:00:00Z");
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    private static final Instant RESOLVED_AT = SCHEDULED_AT.plus(Duration.ofHours(1));
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long SOURCE_POST_1 = 501L;
    private static final Long SOURCE_POST_2 = 502L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_ID = 31L;
    private static final Long ITEM_ID_2 = 32L;
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
        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_1, "우사기 미니 피규어", null, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_2, "하치와레 키링", null, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
        userPort.register(new UserInfo(REQUESTER_ID, "바꾸", 0));
        userPort.register(new UserInfo(OWNER_ID, "하치", 0));
    }

    // ---- AC-12 ----

    @Test
    @DisplayName("NEW · ACTIVE 교환에 확정 약속이 없으면 요청자에게 새로 작성한 제품 editable true")
    void handle_requesterNoConfirmed_isEditable() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        assertTrue(onlyItem(viewAt(NOW, REQUESTER_ID)).editable());
    }

    @Test
    @DisplayName("진행 중 제안(PROPOSED)만 있으면 요청자에게 editable true")
    void handle_requesterWithPendingProposal_isEditable() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED));

        assertTrue(onlyItem(viewAt(NOW, REQUESTER_ID)).editable());
    }

    @Test
    @DisplayName("게시자가 E-2로 보면 새로 작성한 제품 editable false")
    void handle_owner_isNotEditable() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        assertFalse(onlyItem(viewAt(NOW, OWNER_ID)).editable());
    }

    @Test
    @DisplayName("POST 방식 교환이면 요청자가 봐도 모든 제품 editable false")
    void handle_postOffer_allItemsNotEditableForRequester() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, SOURCE_POST_1, null, null, null, 0),
                        OfferedItem.of(ITEM_ID_2, SOURCE_POST_2, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));

        ExchangeResult result = viewAt(NOW, REQUESTER_ID);

        assertEquals(2, result.offeredItems().size());
        result.offeredItems().forEach(item -> assertFalse(item.editable()));
    }

    // ---- AC-13 ----

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L})
    @DisplayName("이 교환의 약속이 CONFIRMED면 교환 확인 전후 모두 요청자에게도 editable false")
    void handle_confirmed_isNotEditableForRequester(long secondsFromOpen) {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED));

        assertFalse(onlyItem(viewAt(OPENS_AT.plusSeconds(secondsFromOpen), REQUESTER_ID)).editable());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELED", "NOT_MET"})
    @DisplayName("확정 약속이 취소되거나 NOT_MET이 되면 요청자에게 다시 editable true")
    void handle_afterCanceledOrNotMet_isEditableAgain(AppointmentStatus status) {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(status));

        assertTrue(onlyItem(viewAt(RESOLVED_AT, REQUESTER_ID)).editable());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("끝난(COMPLETED · CLOSED · LEFT) 교환이면 요청자에게 editable false")
    void handle_endedExchange_isNotEditableForRequester(ExchangeStatus status) {
        exchangeRepository.preload(newItemExchange(status));

        assertFalse(onlyItem(viewAt(RESOLVED_AT, REQUESTER_ID)).editable());
    }

    private ExchangeResult viewAt(Instant now, Long viewerId) {
        GetExchangeHandler handler = new GetExchangeHandler(exchangeRepository, appointmentRepository, postPort,
                userPort, new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul"))), new FakeChatRoomPort());
        return handler.handle(new GetExchangeQuery(EXCHANGE_ID, viewerId));
    }

    private static OfferedItemResult onlyItem(ExchangeResult result) {
        assertEquals(1, result.offeredItems().size());
        OfferedItemResult item = result.offeredItems().get(0);
        assertEquals(ITEM_ID, item.id());
        assertEquals(OfferType.NEW, item.type());
        return item;
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? RESOLVED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    private static Appointment appointment(AppointmentStatus status) {
        boolean pending = status == AppointmentStatus.PROPOSED;
        Instant confirmedAt = pending ? null : CONFIRMED_AT;
        Instant resolvedAt = pending || status == AppointmentStatus.CONFIRMED ? null : RESOLVED_AT;
        return Appointment.of(500L, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(ITEM_ID)), EXPIRES_AT, 30, 30, confirmedAt, resolvedAt, List.of(),
                CREATED_AT, CREATED_AT);
    }
}
