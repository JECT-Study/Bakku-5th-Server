package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.OfferedItemResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.dto.query.GetCurrentAppointmentQuery;
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
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * E-2 · A-2 · A-3 결과가 NEW 아이템의 사진 key를 저장 순서대로 담는지 확인한다 (#108).
 */
class ExchangeImageResultTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T04:50:00Z");
    private static final Long EXCHANGE_X = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long POST_P = 10L;
    private static final Long POST_Q1 = 20L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long ITEM_ID = 31L;
    private static final String NAME = "치이카와 아크릴 스탠드";
    private static final String K1_FINAL = "exchange/1/0b1c2d3e-0001.jpg";
    private static final String K2_FINAL = "exchange/1/0b1c2d3e-0002.jpg";
    private static final String K3_FINAL = "exchange/1/0b1c2d3e-0003.jpg";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private GetExchangeHandler getExchangeHandler;
    private GetAppointmentHandler getAppointmentHandler;
    private GetCurrentAppointmentHandler getCurrentAppointmentHandler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        FakePostPort postPort = new FakePostPort();
        FakeUserPort userPort = new FakeUserPort();
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        getExchangeHandler = new GetExchangeHandler(exchangeRepository, appointmentRepository, postPort, userPort,
                serviceTime, new FakeChatRoomPort());
        getAppointmentHandler = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                serviceTime);
        getCurrentAppointmentHandler = new GetCurrentAppointmentHandler(appointmentRepository, exchangeRepository,
                postPort, serviceTime);

        postPort.register(new PostInfo(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_Q1, "Q1", "post/20/1.jpg", MEMBER_A, PostInfo.Status.AVAILABLE));
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 0));
        userPort.register(new UserInfo(MEMBER_B, "하치", 0));
    }

    // ---- AC-8 ----

    @ParameterizedTest
    @ValueSource(longs = {101L, 102L})
    @DisplayName("요청자 A · 게시자 B가 E-2로 보면 NEW 아이템 사진 key가 저장 순서대로 담긴다")
    void getExchange_newItem_carriesImageKeysInOrder(Long viewerId) {
        exchangeRepository.preload(newItemExchange(List.of(K2_FINAL, K1_FINAL, K3_FINAL)));

        ExchangeResult result = getExchangeHandler.handle(new GetExchangeQuery(EXCHANGE_X, viewerId));

        assertEquals(1, result.offeredItems().size());
        assertEquals(List.of(K2_FINAL, K1_FINAL, K3_FINAL), result.offeredItems().get(0).imageKeys());
    }

    // ---- AC-10 ----

    @Test
    @DisplayName("POST 방식 교환을 E-2로 보면 첨부 아이템 사진 key는 null이다")
    void getExchange_postItem_hasNullImageKeys() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, POST_Q1, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        ExchangeResult result = getExchangeHandler.handle(new GetExchangeQuery(EXCHANGE_X, MEMBER_A));

        OfferedItemResult item = result.offeredItems().get(0);
        assertEquals(OfferType.POST, item.type());
        assertNull(item.imageKeys());
    }

    // ---- AC-9 ----

    @Test
    @DisplayName("NEW 아이템을 고른 약속의 A-2 아이템 사진 key는 첫 사진 K1이다")
    void getAppointment_newItem_thumbnailIsFirstImage() {
        exchangeRepository.preload(newItemExchange(List.of(K1_FINAL, K2_FINAL)));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED, EXPIRES_AT));

        AppointmentResult result = getAppointmentHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertEquals(List.of(new AppointmentItemResult(ITEM_ID, OfferType.NEW, NAME, K1_FINAL)), result.items());
    }

    @Test
    @DisplayName("사진 순서가 [K2, K3]로 바뀐 뒤 A-2 아이템 사진 key는 K2다")
    void getAppointment_afterReorder_thumbnailIsNewFirstImage() {
        exchangeRepository.preload(newItemExchange(List.of(K2_FINAL, K3_FINAL)));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED, EXPIRES_AT));

        AppointmentResult result = getAppointmentHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_B));

        assertEquals(K2_FINAL, result.items().get(0).thumbnailKey());
    }

    @Test
    @DisplayName("사진이 없는 NEW 아이템이면 A-2 아이템 사진 key는 null이다")
    void getAppointment_newItemWithoutImages_hasNullThumbnail() {
        exchangeRepository.preload(newItemExchange(List.of()));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED, EXPIRES_AT));

        AppointmentResult result = getAppointmentHandler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertNull(result.items().get(0).thumbnailKey());
    }

    @Test
    @DisplayName("NEW 아이템을 고른 확정 약속의 A-3 현재 약속 아이템 사진 key는 첫 사진 K1이다")
    void getCurrentAppointment_newItem_thumbnailIsFirstImage() {
        exchangeRepository.preload(newItemExchange(List.of(K1_FINAL, K2_FINAL)));
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, null));

        AppointmentResult result = getCurrentAppointmentHandler
                .handle(new GetCurrentAppointmentQuery(EXCHANGE_X, MEMBER_B))
                .orElseThrow();

        assertEquals(K1_FINAL, result.items().get(0).thumbnailKey());
    }

    private static Exchange newItemExchange(List<String> imageKeys) {
        return Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(NAME), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0, imageKeys)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }

    private static Appointment appointment(AppointmentStatus status, Instant expiresAt) {
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? CREATED_AT : null;
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(ITEM_ID)), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }
}
