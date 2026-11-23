package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.UpdateReminderCommand;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class AppointmentReminderControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final String REMINDER_URL = "/api/v1/appointments/{appointmentId}/reminder";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    /** 제안자 A */
    private static final Long REQUESTER_ID = 100L;
    /** 받는 사람 B */
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_31 = 31L;
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-26 17:00 (+09:00) */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-26T08:00:00Z");
    private static final String THUMBNAIL = "post/501/1.jpg";
    private static final String THUMBNAIL_URL = "https://img.test/api/v1/images/post/501/1.jpg";

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        commandBus.onUpdateReminder(command -> APPOINTMENT_ID);
        queryBus.willReturnAppointment(confirmedResult(10));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AppointmentController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- 정상 흐름 (AC-1 · AC-2) ----

    @Test
    @DisplayName("A-7 제안자가 알림을 10으로 바꾸면 200 · CONFIRMED · 내 알림 10이다")
    void updateReminder_byProposer_returns200() throws Exception {
        mockMvc.perform(reminderRequest(REQUESTER_ID, "{\"reminderOffsetMinutes\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.exchangeId").value(EXCHANGE_ID))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-27T15:00:00+09:00"))
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-10-26T17:00:00+09:00"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(10));
    }

    @Test
    @DisplayName("A-7 경로 id · 로그인 회원 · 알림 값이 커맨드로 가고 반환 id와 변경자로 조회한다")
    void updateReminder_sendsCommandThenQueryAsChanger() throws Exception {
        mockMvc.perform(reminderRequest(REQUESTER_ID, "{\"reminderOffsetMinutes\": 10}"))
                .andExpect(status().isOk());

        assertEquals(1, commandBus.sentCommands().size());
        UpdateReminderCommand command = assertInstanceOf(UpdateReminderCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(REQUESTER_ID, command.memberId());
        assertEquals(10, command.reminderOffsetMinutes());

        assertEquals(1, queryBus.sentQueries().size());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(REQUESTER_ID, query.viewerId());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 1440})
    @DisplayName("A-7 알림 경계값 10 · 1440이면 200이고 그 값이 커맨드로 간다")
    void updateReminder_boundaryValue_returns200(int reminder) throws Exception {
        mockMvc.perform(reminderRequest(REQUESTER_ID, "{\"reminderOffsetMinutes\": " + reminder + "}"))
                .andExpect(status().isOk());

        UpdateReminderCommand command = (UpdateReminderCommand) commandBus.sentCommands().get(0);
        assertEquals(reminder, command.reminderOffsetMinutes());
    }

    @Test
    @DisplayName("A-7 받는 사람이 알림을 1440으로 바꾸면 200 · 내 알림 1440이고 받는 사람 기준으로 조회한다")
    void updateReminder_byReceiver_returns200AsReceiver() throws Exception {
        queryBus.willReturnAppointment(confirmedResult(1440));

        mockMvc.perform(reminderRequest(OWNER_ID, "{\"reminderOffsetMinutes\": 1440}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(1440));

        UpdateReminderCommand command = (UpdateReminderCommand) commandBus.sentCommands().get(0);
        assertEquals(OWNER_ID, command.memberId());
        assertEquals(1440, command.reminderOffsetMinutes());
        GetAppointmentQuery query = (GetAppointmentQuery) queryBus.sentQueries().get(0);
        assertEquals(OWNER_ID, query.viewerId());
    }

    // ---- 끄기 (AC-3) ----

    @ParameterizedTest
    @ValueSource(strings = {"{\"reminderOffsetMinutes\": null}", "{}"})
    @DisplayName("A-7 알림 null 또는 필드 생략이면 200이고 커맨드 알림은 null이다")
    void updateReminder_nullOrOmitted_sendsNull(String body) throws Exception {
        queryBus.willReturnAppointment(confirmedResult(null));

        mockMvc.perform(reminderRequest(REQUESTER_ID, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").doesNotExist());

        UpdateReminderCommand command = assertInstanceOf(UpdateReminderCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(REQUESTER_ID, command.memberId());
        assertNull(command.reminderOffsetMinutes());
    }

    // ---- 값 형식 (AC-5) ----

    @ParameterizedTest
    @ValueSource(ints = {5, 9, 0, -10, 1441, 1450, 15, 1435})
    @DisplayName("A-7 알림 값이 10분 단위 10~1440이 아니면 400 COMMON_001이고 커맨드를 보내지 않는다")
    void updateReminder_invalidValue_returnsCommon001(int reminder) throws Exception {
        mockMvc.perform(reminderRequest(REQUESTER_ID, "{\"reminderOffsetMinutes\": " + reminder + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("reminderOffsetMinutes")));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    @Test
    @DisplayName("A-7 본문이 없으면 400 COMMON_002이고 커맨드를 보내지 않는다")
    void updateReminder_withoutBody_returnsCommon002() throws Exception {
        mockMvc.perform(put(REMINDER_URL, APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_002"));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    // ---- 인증 (AC-6) ----

    @Test
    @DisplayName("A-7 로그인 회원 id가 없으면 401 AUTH_001이고 커맨드를 보내지 않는다")
    void updateReminder_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(put(REMINDER_URL, APPOINTMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOffsetMinutes\": 10}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("A-7 로그인 회원 id가 없고 알림 값도 틀리면 401 AUTH_001이 먼저다")
    void updateReminder_withoutLoginMemberAndInvalidValue_returnsAuth001() throws Exception {
        mockMvc.perform(put(REMINDER_URL, APPOINTMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOffsetMinutes\": 5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    // ---- 처리 실패 응답 (AC-7 ~ AC-12) ----

    @Test
    @DisplayName("A-7 없는 약속이면 404 EXCHANGE_101이고 조회하지 않는다")
    void updateReminder_missing_returnsExchange101() throws Exception {
        assertFailure(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, 404);
    }

    @Test
    @DisplayName("A-7 참여자가 아니면 403 EXCHANGE_002")
    void updateReminder_thirdParty_returnsExchange002() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, 403);
    }

    @Test
    @DisplayName("A-7 확정이 아닌 약속이면 409 EXCHANGE_109")
    void updateReminder_notConfirmed_returnsExchange109() throws Exception {
        assertFailure(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, 409);
    }

    @Test
    @DisplayName("A-7 약속 시각이 지났으면 409 EXCHANGE_107")
    void updateReminder_scheduledAtPassed_returnsExchange107() throws Exception {
        assertFailure(ExchangeErrorCode.SCHEDULED_AT_PASSED, 409);
    }

    @Test
    @DisplayName("A-7 지금 바로 약속이면 409 EXCHANGE_110")
    void updateReminder_nowAppointment_returnsExchange110() throws Exception {
        assertFailure(ExchangeErrorCode.REMINDER_NOT_ALLOWED, 409);
    }

    @Test
    @DisplayName("A-7 종료된 교환이면 409 EXCHANGE_010")
    void updateReminder_exchangeClosed_returnsExchange010() throws Exception {
        assertFailure(ExchangeErrorCode.EXCHANGE_CLOSED, 409);
    }

    // ---- helpers ----

    private void assertFailure(ExchangeErrorCode errorCode, int httpStatus) throws Exception {
        commandBus.onUpdateReminder(command -> {
            throw new ExchangeException(errorCode);
        });

        mockMvc.perform(reminderRequest(REQUESTER_ID, "{\"reminderOffsetMinutes\": 10}"))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    private static MockHttpServletRequestBuilder reminderRequest(Long memberId, String body) {
        return put(REMINDER_URL, APPOINTMENT_ID)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** 변경자 기준 확정 약속 조회 결과 */
    private static AppointmentResult confirmedResult(Integer myReminder) {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, myReminder, false, CONFIRMED_AT, CREATED_AT);
    }
}
