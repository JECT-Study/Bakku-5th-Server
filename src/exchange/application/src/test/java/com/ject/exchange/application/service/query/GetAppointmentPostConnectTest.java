package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A-2 약속 아이템이 post BC 글의 제목 · 사진 key를 담고, 삭제되거나 없는 글은 null로 두는지 확인한다 (#107).
 */
class GetAppointmentPostConnectTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T04:50:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_X = 1L;
    private static final Long POST_P = 10L;
    private static final Long POST_Q1 = 20L;
    private static final Long POST_Q2 = 21L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final String KEY_Q1 = "post/20/1.jpg";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private GetAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postPort = new FakePostPort();
        handler = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        postPort.register(new PostInfo(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_Q2, "Q2", null, MEMBER_A, PostInfo.Status.AVAILABLE));
    }

    @Test
    @DisplayName("Q1을 고른 약속의 아이템은 Q1 제목과 사진 key를 담는다")
    void handle_postItem_carriesTitleAndThumbnailKey() {
        givenProposal(List.of(ITEM_Q1));
        postPort.register(new PostInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.AVAILABLE));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertEquals(List.of(new AppointmentItemResult(ITEM_Q1, OfferType.POST, "Q1", KEY_Q1)), result.items());
    }

    @Test
    @DisplayName("사진 key가 없는 글이면 아이템 사진 key는 null이다")
    void handle_postItemWithoutKey_hasNullThumbnailKey() {
        givenProposal(List.of(ITEM_Q2));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertEquals(List.of(new AppointmentItemResult(ITEM_Q2, OfferType.POST, "Q2", null)), result.items());
    }

    @Test
    @DisplayName("Q1이 삭제됐으면 post가 제목 · key를 줘도 아이템 이름 · 사진 key는 null이다")
    void handle_deletedPostItem_hidesTitleAndKey() {
        givenProposal(List.of(ITEM_Q1, ITEM_Q2));
        postPort.register(new PostInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.DELETED));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertEquals(List.of(
                new AppointmentItemResult(ITEM_Q1, OfferType.POST, null, null),
                new AppointmentItemResult(ITEM_Q2, OfferType.POST, "Q2", null)), result.items());
    }

    @Test
    @DisplayName("Q1이 post BC에 없으면 아이템 이름 · 사진 key는 null이다")
    void handle_missingPostItem_hasNullTitleAndKey() {
        givenProposal(List.of(ITEM_Q1));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_B));

        assertEquals(List.of(new AppointmentItemResult(ITEM_Q1, OfferType.POST, null, null)), result.items());
    }

    @Test
    @DisplayName("새로 작성한 아이템은 제품명을 담고 사진 key는 null이다")
    void handle_newItem_hasNullThumbnailKey() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_Q1, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
        appointmentRepository.preload(appointment(List.of(ITEM_Q1)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, MEMBER_A));

        assertEquals(List.of(new AppointmentItemResult(ITEM_Q1, OfferType.NEW, "치이카와 아크릴 스탠드", null)),
                result.items());
    }

    private void givenProposal(List<Long> itemIds) {
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0),
                        OfferedItem.of(ITEM_Q2, POST_Q2, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
        appointmentRepository.preload(appointment(itemIds));
    }

    private static Appointment appointment(List<Long> itemIds) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B, AppointmentStatus.PROPOSED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(itemIds), EXPIRES_AT, 30, null, null, CREATED_AT, CREATED_AT);
    }
}
