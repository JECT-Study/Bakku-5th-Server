package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class AppointmentDetailControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final String DETAIL_URL = "/api/v1/appointments/{appointmentId}";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-27 14:00 (+09:00) */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-26 17:00 (+09:00) */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-26T08:00:00Z");
    private static final String PLACE_NAME = "홍대입구역 3번 출구 앞 편의점";
    private static final String PLACE_ADDRESS = "서울 마포구 양화로 160";
    private static final String THUMBNAIL = "post/501/1.jpg";
    private static final String THUMBNAIL_URL = "https://img.test/api/v1/images/post/501/1.jpg";

    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AppointmentController(new FakeCommandBus(), queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("A-2 제안자가 조회하면 200과 약속 전체 필드를 반환한다")
    void getAppointment_byProposer_returns200WithAllFields() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.PROPOSED, EXPIRES_AT, 30, null));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.exchangeId").value(EXCHANGE_ID))
                .andExpect(jsonPath("$.data.proposerUserId").value(REQUESTER_ID))
                .andExpect(jsonPath("$.data.receiverUserId").value(OWNER_ID))
                .andExpect(jsonPath("$.data.status").value("PROPOSED"))
                .andExpect(jsonPath("$.data.timeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-27T15:00:00+09:00"))
                .andExpect(jsonPath("$.data.place.name").value(PLACE_NAME))
                .andExpect(jsonPath("$.data.place.address").value(PLACE_ADDRESS))
                .andExpect(jsonPath("$.data.place.lat").value(37.5571))
                .andExpect(jsonPath("$.data.place.lng").value(126.9245))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].offeredItemId").value(ITEM_31))
                .andExpect(jsonPath("$.data.items[0].type").value("POST"))
                .andExpect(jsonPath("$.data.items[0].name").value("우사기 미니 피규어"))
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value(THUMBNAIL_URL))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-10-27T14:00:00+09:00"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(30))
                .andExpect(jsonPath("$.data.otherActiveProposalsOnPost").value(false))
                .andExpect(jsonPath("$.data.pendingChange").doesNotExist())
                .andExpect(jsonPath("$.data.pendingCancellation").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation").doesNotExist())
                .andExpect(jsonPath("$.data.confirmedAt").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").value("2026-10-26T14:00:00+09:00"));
    }

    @Test
    @DisplayName("A-2 경로의 약속 id와 로그인 회원이 조회 쿼리로 전달된다")
    void getAppointment_byProposer_sendsQueryWithViewer() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.PROPOSED, EXPIRES_AT, 30, null));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk());

        assertEquals(1, queryBus.sentQueries().size());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(REQUESTER_ID, query.viewerId());
    }

    @Test
    @DisplayName("A-2 받는 사람이 조회하면 조회자가 받는 사람으로 전달되고 내 알림은 null이다")
    void getAppointment_byReceiver_sendsReceiverAndReturnsNullReminder() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.PROPOSED, EXPIRES_AT, null, null));

        mockMvc.perform(detailRequest(OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.status").value("PROPOSED"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").doesNotExist());

        GetAppointmentQuery query = (GetAppointmentQuery) queryBus.sentQueries().get(0);
        assertEquals(OWNER_ID, query.viewerId());
    }

    @Test
    @DisplayName("A-2 만료로 보이는 제안이면 status EXPIRED · expiresAt null로 응답한다")
    void getAppointment_expired_returnsExpiredStatus() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.EXPIRED, null, 30, null));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"))
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
    }

    @Test
    @DisplayName("A-2 확정된 약속이면 confirmedAt이 +09:00 시각이고 아직 없는 기능 필드 3개는 null이다")
    void getAppointment_confirmed_returnsConfirmedAtAndNullPendingFields() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.CONFIRMED, null, 30, CONFIRMED_AT));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-10-26T17:00:00+09:00"))
                .andExpect(jsonPath("$.data.pendingChange").doesNotExist())
                .andExpect(jsonPath("$.data.pendingCancellation").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation").doesNotExist());
    }

    // ---- 실패 ----

    @Test
    @DisplayName("A-2 없는 약속이면 404 EXCHANGE_101")
    void getAppointment_missing_returnsExchange101() throws Exception {
        queryBus.willFailAppointment(new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_101"));
    }

    @Test
    @DisplayName("A-2 참여자가 아니면 403 EXCHANGE_002")
    void getAppointment_thirdParty_returnsExchange002() throws Exception {
        queryBus.willFailAppointment(new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT));

        mockMvc.perform(detailRequest(OTHER_MEMBER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_002"));
    }

    @Test
    @DisplayName("A-2 로그인 회원 id가 없으면 401 AUTH_001이고 쿼리를 보내지 않는다")
    void getAppointment_withoutLoginMember_returnsAuth001() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.PROPOSED, EXPIRES_AT, 30, null));

        mockMvc.perform(get(DETAIL_URL, APPOINTMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    // ---- helpers ----

    private static MockHttpServletRequestBuilder detailRequest(Long memberId) {
        return get(DETAIL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, memberId);
    }

    private static AppointmentResult result(AppointmentStatus status, Instant expiresAt, Integer reminder,
                                            Instant confirmedAt) {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT, new Place(PLACE_NAME, PLACE_ADDRESS, 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                expiresAt, reminder, false, confirmedAt, CREATED_AT);
    }
}
