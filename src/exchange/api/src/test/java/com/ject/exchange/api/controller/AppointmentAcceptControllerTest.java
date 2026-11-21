package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class AppointmentAcceptControllerTest {

    private static final String ACCEPT_URL = "/api/v1/appointments/{appointmentId}/accept";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_31 = 31L;
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-26 17:00 (+09:00) */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-26T08:00:00Z");
    private static final String THUMBNAIL = "https://cdn.example.com/post/501/1.jpg";

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        commandBus.onAcceptAppointment(command -> APPOINTMENT_ID);
        queryBus.willReturnAppointment(confirmedResult(60));
        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(commandBus, queryBus))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("A-4 받는 사람이 수락하면 200 · CONFIRMED · 확정 시각 +09:00 · 만료 null이다")
    void acceptAppointment_byReceiver_returns200Confirmed() throws Exception {
        mockMvc.perform(acceptRequest(OWNER_ID, "{\"reminderOffsetMinutes\": 60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.exchangeId").value(EXCHANGE_ID))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-27T15:00:00+09:00"))
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-10-26T17:00:00+09:00"))
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
    }

    @Test
    @DisplayName("A-4 경로 id · 로그인 회원 · 알림 값이 커맨드로 가고 반환 id와 수락자로 조회한다")
    void acceptAppointment_sendsCommandThenQueryAsAccepter() throws Exception {
        mockMvc.perform(acceptRequest(OWNER_ID, "{\"reminderOffsetMinutes\": 60}"))
                .andExpect(status().isOk());

        assertEquals(1, commandBus.sentCommands().size());
        AcceptAppointmentCommand command = assertInstanceOf(AcceptAppointmentCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(OWNER_ID, command.accepterId());
        assertEquals(60, command.reminderOffsetMinutes());

        assertEquals(1, queryBus.sentQueries().size());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(OWNER_ID, query.viewerId());
    }

    @Test
    @DisplayName("A-4 응답의 내 알림은 수락자 기준 값 60이다")
    void acceptAppointment_returnsAccepterReminder() throws Exception {
        mockMvc.perform(acceptRequest(OWNER_ID, "{\"reminderOffsetMinutes\": 60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(60));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 1440})
    @DisplayName("A-4 알림 경계값 10 · 1440이면 200이고 그 값이 커맨드로 간다")
    void acceptAppointment_boundaryReminder_returns200(int reminder) throws Exception {
        mockMvc.perform(acceptRequest(OWNER_ID, "{\"reminderOffsetMinutes\": " + reminder + "}"))
                .andExpect(status().isOk());

        AcceptAppointmentCommand command = (AcceptAppointmentCommand) commandBus.sentCommands().get(0);
        assertEquals(reminder, command.reminderOffsetMinutes());
    }

    @Test
    @DisplayName("A-4 본문 없이 수락하면 200이고 커맨드 알림은 null이다")
    void acceptAppointment_withoutBody_sendsNullReminder() throws Exception {
        queryBus.willReturnAppointment(confirmedResult(null));

        mockMvc.perform(post(ACCEPT_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").doesNotExist());

        AcceptAppointmentCommand command = assertInstanceOf(AcceptAppointmentCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(OWNER_ID, command.accepterId());
        assertNull(command.reminderOffsetMinutes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"reminderOffsetMinutes\": null}", "{}"})
    @DisplayName("A-4 알림 null 또는 빈 본문이면 200이고 커맨드 알림은 null이다")
    void acceptAppointment_nullReminder_sendsNullReminder(String body) throws Exception {
        queryBus.willReturnAppointment(confirmedResult(null));

        mockMvc.perform(acceptRequest(OWNER_ID, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        AcceptAppointmentCommand command = (AcceptAppointmentCommand) commandBus.sentCommands().get(0);
        assertNull(command.reminderOffsetMinutes());
    }

    // ---- 입력 형식 ----

    @ParameterizedTest
    @ValueSource(ints = {5, 9, 0, -10, 1441, 1450, 15, 1435})
    @DisplayName("A-4 알림 값이 10분 단위 10~1440이 아니면 400 COMMON_001이고 커맨드를 보내지 않는다")
    void acceptAppointment_invalidReminder_returnsCommon001(int reminder) throws Exception {
        mockMvc.perform(acceptRequest(OWNER_ID, "{\"reminderOffsetMinutes\": " + reminder + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("reminderOffsetMinutes")));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    // ---- 인증 ----

    @Test
    @DisplayName("A-4 로그인 회원 id가 없으면 401 AUTH_001이고 커맨드를 보내지 않는다")
    void acceptAppointment_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(post(ACCEPT_URL, APPOINTMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOffsetMinutes\": 60}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("A-4 본문 없이 로그인 회원 id도 없으면 401 AUTH_001")
    void acceptAppointment_withoutLoginMemberAndBody_returnsAuth001() throws Exception {
        mockMvc.perform(post(ACCEPT_URL, APPOINTMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    // ---- 처리 실패 응답 ----

    @Test
    @DisplayName("A-4 없는 약속이면 404 EXCHANGE_101이고 조회하지 않는다")
    void acceptAppointment_missing_returnsExchange101() throws Exception {
        assertFailure(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, 404);
    }

    @Test
    @DisplayName("A-4 참여자가 아니면 403 EXCHANGE_002")
    void acceptAppointment_thirdParty_returnsExchange002() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, 403);
    }

    @Test
    @DisplayName("A-4 제안자가 수락하면 403 EXCHANGE_104")
    void acceptAppointment_byProposer_returnsExchange104() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, 403);
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeErrorCode.class, names = {"PROPOSAL_NOT_ACTIVE", "SCHEDULED_AT_PASSED",
            "POST_RESERVED_ELSEWHERE", "POST_NOT_REQUESTABLE", "ITEM_NOT_AVAILABLE", "EXCHANGE_CLOSED"})
    @DisplayName("A-4 수락 판정 충돌이면 409와 해당 에러 코드다")
    void acceptAppointment_conflict_returns409(ExchangeErrorCode errorCode) throws Exception {
        assertFailure(errorCode, 409);
    }

    // ---- helpers ----

    private void assertFailure(ExchangeErrorCode errorCode, int httpStatus) throws Exception {
        commandBus.onAcceptAppointment(command -> {
            throw new ExchangeException(errorCode);
        });

        mockMvc.perform(acceptRequest(OWNER_ID, "{\"reminderOffsetMinutes\": 60}"))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    private static MockHttpServletRequestBuilder acceptRequest(Long memberId, String body) {
        return post(ACCEPT_URL, APPOINTMENT_ID)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** 수락자(받는 사람) 기준 확정 약속 조회 결과 */
    private static AppointmentResult confirmedResult(Integer accepterReminder) {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, accepterReminder, false, CONFIRMED_AT, CREATED_AT);
    }
}
