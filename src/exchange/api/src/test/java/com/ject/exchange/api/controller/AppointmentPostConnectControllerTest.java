package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.port.PostStatusRejectedException;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.OfferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 약속 응답의 아이템 사진 key를 URL로 바꾸는지, F-1 완료 · 해제 거부가 500이 되는지 확인한다 (#107).
 */
class AppointmentPostConnectControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");
    private static final String DETAIL_URL = "/api/v1/appointments/{appointmentId}";
    private static final String CONFIRMATION_URL = "/api/v1/appointments/{appointmentId}/confirmation";
    private static final Long EXCHANGE_X = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long POST_P = 10L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final String KEY_Q1 = "post/20/1.jpg";
    private static final String URL_Q1 = "https://img.test/api/v1/images/post/20/1.jpg";
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- AC-6 ----

    @Test
    @DisplayName("A-2 Q1 아이템의 대표 사진은 key로 만든 URL이다")
    void getAppointment_postItem_resolvesThumbnailUrl() throws Exception {
        queryBus.willReturnAppointment(result(List.of(
                new AppointmentItemResult(ITEM_Q1, OfferType.POST, "Q1", KEY_Q1))));

        mockMvc.perform(get(DETAIL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].offeredItemId").value(ITEM_Q1))
                .andExpect(jsonPath("$.data.items[0].name").value("Q1"))
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value(URL_Q1));
    }

    @Test
    @DisplayName("A-2 사진 key가 null인 아이템의 대표 사진은 null이다")
    void getAppointment_nullKey_returnsNullThumbnailUrl() throws Exception {
        queryBus.willReturnAppointment(result(List.of(
                new AppointmentItemResult(ITEM_Q1, OfferType.POST, "Q1", KEY_Q1),
                new AppointmentItemResult(ITEM_Q2, OfferType.POST, "Q2", null))));

        mockMvc.perform(get(DETAIL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value(URL_Q1))
                .andExpect(jsonPath("$.data.items[1].name").value("Q2"))
                .andExpect(jsonPath("$.data.items[1].thumbnailUrl").doesNotExist());
    }

    // ---- AC-7 ----

    @Test
    @DisplayName("A-2 삭제된 글 아이템은 이름 · 대표 사진이 null이다")
    void getAppointment_deletedPostItem_returnsNullNameAndThumbnail() throws Exception {
        queryBus.willReturnAppointment(result(List.of(
                new AppointmentItemResult(ITEM_Q1, OfferType.POST, null, null))));

        mockMvc.perform(get(DETAIL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, MEMBER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].offeredItemId").value(ITEM_Q1))
                .andExpect(jsonPath("$.data.items[0].type").value("POST"))
                .andExpect(jsonPath("$.data.items[0].name").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").doesNotExist());
    }

    // ---- AC-16 ----

    @Test
    @DisplayName("F-1 post BC가 완료 · 해제를 거부하면 500 INTERNAL_001이다")
    void respondConfirmation_postStatusRejected_returnsInternal001() throws Exception {
        commandBus.onRespondConfirmation(command -> {
            throw new PostStatusRejectedException(POST_P);
        });

        mockMvc.perform(post(CONFIRMATION_URL, APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, MEMBER_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\": \"DONE\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_001"));
    }

    private static AppointmentResult result(List<AppointmentItemResult> items) {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B, AppointmentStatus.PROPOSED,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245),
                items, EXPIRES_AT, 30, false, null, CREATED_AT);
    }
}
