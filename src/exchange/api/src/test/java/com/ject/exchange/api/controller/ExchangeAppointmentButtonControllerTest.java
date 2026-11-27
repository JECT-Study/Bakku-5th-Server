package com.ject.exchange.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.service.command.CreateExchangeHandler;
import com.ject.exchange.application.service.command.UpdateNewItemHandler;
import com.ject.exchange.application.service.query.GetExchangeHandler;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E-1 · E-2 · E-3 응답의 {@code appointmentButton} · {@code offeredItems[].editable}과 E-3 EXCHANGE_014를 확인한다 (#120).
 * 버스는 실제 Handler로 넘기고, 약속 · 글 · 회원은 Fake로 만든다.
 */
class ExchangeAppointmentButtonControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final Instant CREATED_AT = Instant.parse("2026-11-24T03:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-24T04:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-24T05:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plus(Duration.ofDays(1));
    /** 교환 확인이 아직 열리지 않은 약속 시각 */
    private static final Instant FUTURE_SCHEDULED_AT = NOW.plus(Duration.ofDays(1));
    /** 교환 확인이 이미 열린 약속 시각 (+30분이 NOW보다 앞) */
    private static final Instant PAST_SCHEDULED_AT = NOW.minus(Duration.ofHours(1));
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long SOURCE_POST_ID = 21L;
    private static final Long REQUESTER_ID = 101L;
    private static final Long OWNER_ID = 102L;
    private static final Long ITEM_ID = 31L;
    private static final Long APPOINTMENT_ID = 500L;
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String DETAIL_URL = "/api/v1/exchanges/{exchangeId}";
    private static final String UPDATE_URL = "/api/v1/exchanges/{exchangeId}/new-item";
    private static final String CREATE_URL = "/api/v1/exchanges";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private FakeImagePort imagePort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, CREATED_AT);
        postPort = new FakePostPort();
        imagePort = new FakeImagePort();
        FakeUserPort userPort = new FakeUserPort();
        FakeCommandBus commandBus = new FakeCommandBus(
                new UpdateNewItemHandler(exchangeRepository, appointmentRepository, new FakePostLockRepository(),
                        imagePort),
                new CreateExchangeHandler(exchangeRepository, postPort, userPort, imagePort, new FakeChatRoomPort()));
        FakeQueryBus queryBus = new FakeQueryBus(new GetExchangeHandler(exchangeRepository, appointmentRepository,
                postPort, userPort, new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))), new FakeChatRoomPort()));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ExchangeController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        givenPost(PostInfo.Status.AVAILABLE);
        postPort.register(new PostInfo(SOURCE_POST_ID, "우사기 미니 피규어", null, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
        userPort.register(new UserInfo(REQUESTER_ID, "바꾸", 0));
        userPort.register(new UserInfo(OWNER_ID, "하치", 0));
    }

    // ---- E-3 잠금 (AC-1 · AC-2 · AC-3) ----

    @Test
    @DisplayName("E-3 확정 약속이 있으면 409 EXCHANGE_014이고 E-2로 보면 제품이 그대로다")
    void updateNewItem_confirmed_returnsExchange014() throws Exception {
        // given
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, FUTURE_SCHEDULED_AT));

        // when & then
        mockMvc.perform(updateRequest(REQUESTER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_014"));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].name").value(ORIGINAL_NAME))
                .andExpect(jsonPath("$.data.offeredItems[0].opened").value(false));
        assertTrue(imagePort.calls().isEmpty());
    }

    @Test
    @DisplayName("E-3 확정 약속이 있어도 게시자가 수정하면 403 EXCHANGE_012")
    void updateNewItem_confirmedByOwner_returnsExchange012() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, FUTURE_SCHEDULED_AT));

        mockMvc.perform(updateRequest(OWNER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXCHANGE_012"));
    }

    @Test
    @DisplayName("E-3 확정 약속이 있는 POST 방식 교환이면 409 EXCHANGE_013")
    void updateNewItem_confirmedPostOffer_returnsExchange013() throws Exception {
        exchangeRepository.save(postOfferExchange());
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, FUTURE_SCHEDULED_AT));

        mockMvc.perform(updateRequest(REQUESTER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_013"));
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("E-3 확정 약속이 있는 교환이 ACTIVE가 아니면 409 EXCHANGE_010")
    void updateNewItem_confirmedNotActive_returnsExchange010(ExchangeStatus status) throws Exception {
        exchangeRepository.save(newItemExchange(status));
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, FUTURE_SCHEDULED_AT));

        mockMvc.perform(updateRequest(REQUESTER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_010"));
    }

    @Test
    @DisplayName("E-3 확정 약속이 없으면 200이고 응답 버튼 PROPOSE · 제품 editable true")
    void updateNewItem_noConfirmed_returns200WithProposeAndEditable() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(AppointmentStatus.PROPOSED, FUTURE_SCHEDULED_AT));

        mockMvc.perform(updateRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].name").value("새 이름"))
                .andExpect(jsonPath("$.data.appointmentButton").value("PROPOSE"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(true));
    }

    // ---- E-1 (AC-5 · AC-12) ----

    @Test
    @DisplayName("E-1 NEW 방식 생성 응답은 201이고 버튼 PROPOSE · 요청자 기준 제품 editable true")
    void createExchange_newItem_returnsProposeAndEditable() throws Exception {
        mockMvc.perform(post(CREATE_URL)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\": " + POST_ID + ", \"offerType\": \"NEW\", \"newItem\": "
                                + "{\"imageKeys\": [\"exchange/tmp/a.jpg\"], \"name\": \"" + ORIGINAL_NAME + "\", "
                                + "\"opened\": false, \"description\": \"가챠에서 두 개 나와서 하나 교환해요\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.appointmentButton").value("PROPOSE"))
                .andExpect(jsonPath("$.data.offeredItems[0].type").value("NEW"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(true));
    }

    // ---- E-2 버튼 (AC-5 ~ AC-11) ----

    @Test
    @DisplayName("E-2 교환 확인 전 확정 약속이면 요청자 · 게시자 모두 CANCEL이고 editable false")
    void getExchange_confirmedBeforeOpen_returnsCancelForBoth() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        givenPost(PostInfo.Status.RESERVED);
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, FUTURE_SCHEDULED_AT));

        for (Long viewerId : List.of(REQUESTER_ID, OWNER_ID)) {
            mockMvc.perform(detailRequest(viewerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.appointmentButton").value("CANCEL"))
                    .andExpect(jsonPath("$.data.offeredItems[0].editable").value(false));
        }
    }

    @Test
    @DisplayName("E-2 교환 확인이 열린 뒤 답이 없는 확정 약속이면 NONE")
    void getExchange_confirmedAfterOpen_returnsNone() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        givenPost(PostInfo.Status.RESERVED);
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, PAST_SCHEDULED_AT));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentButton").value("NONE"));
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("E-2 끝난(COMPLETED · CLOSED · LEFT) 교환이면 NONE이고 요청자 editable false")
    void getExchange_endedExchange_returnsNone(ExchangeStatus status) throws Exception {
        exchangeRepository.save(newItemExchange(status));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentButton").value("NONE"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(false));
    }

    @ParameterizedTest
    @ValueSource(longs = {101L, 102L})
    @DisplayName("E-2 확정 약속 없이 게시글이 다른 교환으로 RESERVED면 요청자 · 게시자 모두 PROPOSE_DISABLED")
    void getExchange_postReservedElsewhere_returnsProposeDisabled(Long viewerId) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        givenPost(PostInfo.Status.RESERVED);

        mockMvc.perform(detailRequest(viewerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentButton").value("PROPOSE_DISABLED"));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CANCELED", "NOT_MET"})
    @DisplayName("E-2 확정 약속이 취소 · 못 만남으로 끝났으면 PROPOSE이고 요청자 editable true")
    void getExchange_afterCanceledOrNotMet_returnsProposeAndEditable(AppointmentStatus status) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        appointmentRepository.preload(appointment(status, PAST_SCHEDULED_AT));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentButton").value("PROPOSE"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(true));
    }

    // ---- E-2 editable (AC-12) ----

    @Test
    @DisplayName("E-2 확정 약속이 없으면 요청자는 editable true · 게시자는 false이고 버튼은 둘 다 PROPOSE")
    void getExchange_noConfirmed_editableOnlyForRequester() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentButton").value("PROPOSE"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(true));
        mockMvc.perform(detailRequest(OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentButton").value("PROPOSE"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(false));
    }

    @Test
    @DisplayName("E-2 POST 방식 교환이면 요청자에게도 editable false")
    void getExchange_postOffer_notEditableForRequester() throws Exception {
        exchangeRepository.save(postOfferExchange());

        mockMvc.perform(detailRequest(REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].type").value("POST"))
                .andExpect(jsonPath("$.data.offeredItems[0].editable").value(false));
    }

    private void givenPost(PostInfo.Status status) {
        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, status));
    }

    private static MockHttpServletRequestBuilder detailRequest(Long memberId) {
        return get(DETAIL_URL, EXCHANGE_ID).requestAttr(LoginMember.ATTRIBUTE, memberId);
    }

    private static MockHttpServletRequestBuilder updateRequest(Long memberId) {
        return put(UPDATE_URL, EXCHANGE_ID)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"imageKeys\": [\"exchange/tmp/a.jpg\"], \"name\": \"새 이름\", "
                        + "\"opened\": true, \"description\": \"새 설명\"}");
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    private static Exchange postOfferExchange() {
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, SOURCE_POST_ID, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);
    }

    private static Appointment appointment(AppointmentStatus status, Instant scheduledAt) {
        boolean pending = status == AppointmentStatus.PROPOSED;
        Instant confirmedAt = pending ? null : CONFIRMED_AT;
        Instant resolvedAt = pending || status == AppointmentStatus.CONFIRMED ? null : NOW.minus(Duration.ofMinutes(5));
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt), PLACE,
                new AppointmentItemIds(List.of(ITEM_ID)), EXPIRES_AT, 30, 30, confirmedAt, resolvedAt, List.of(),
                CREATED_AT, CREATED_AT);
    }
}
