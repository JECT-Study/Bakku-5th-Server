package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.RespondConfirmationCommand;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.ConfirmationSummary;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;
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
import java.time.Duration;
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
class AppointmentConfirmationControllerTest {

    private static final String CONFIRMATION_URL = "/api/v1/appointments/{appointmentId}/confirmation";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    /** 제안자 A */
    private static final Long REQUESTER_ID = 100L;
    /** 받는 사람 B */
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_31 = 31L;
    /** 2026-11-23 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 15:30 (+09:00) */
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    /** 2026-11-23 15:40 (+09:00) */
    private static final Instant RESOLVED_AT = OPENS_AT.plus(Duration.ofMinutes(10));
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final String DONE_BODY = "{\"answer\": \"DONE\"}";

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        commandBus.onRespondConfirmation(command -> APPOINTMENT_ID);
        queryBus.willReturnAppointment(result(AppointmentStatus.COMPLETED,
                new ConfirmationSummary(OPENS_AT, AppointmentStatus.COMPLETED, false, ConfirmationAnswer.DONE,
                        RESOLVED_AT)));
        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(commandBus, queryBus))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- AC-1 먼저 교환했어요 ----

    @Test
    @DisplayName("F-1 A가 DONE으로 답하면 200 · COMPLETED · confirmation 값이 채워진다")
    void respondConfirmation_done_returns200WithConfirmation() throws Exception {
        mockMvc.perform(confirmationRequest(REQUESTER_ID, DONE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.confirmation.askedAt").value("2026-11-23T15:30:00+09:00"))
                .andExpect(jsonPath("$.data.confirmation.result").value("COMPLETED"))
                .andExpect(jsonPath("$.data.confirmation.autoCompleted").value(false))
                .andExpect(jsonPath("$.data.confirmation.myAnswer").value("DONE"))
                .andExpect(jsonPath("$.data.confirmation.resolvedAt").value("2026-11-23T15:40:00+09:00"));
    }

    @Test
    @DisplayName("F-1 경로 id · 로그인 회원 · 답이 커맨드로 가고 반환 id와 응답자로 조회한다")
    void respondConfirmation_sendsCommandThenQueryAsResponder() throws Exception {
        mockMvc.perform(confirmationRequest(REQUESTER_ID, DONE_BODY))
                .andExpect(status().isOk());

        assertEquals(1, commandBus.sentCommands().size());
        RespondConfirmationCommand command = assertInstanceOf(RespondConfirmationCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(REQUESTER_ID, command.memberId());
        assertEquals(ConfirmationAnswer.DONE, command.answer());
        assertNull(command.notMetReason());
        assertNull(command.notMetDetail());

        assertEquals(1, queryBus.sentQueries().size());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(REQUESTER_ID, query.viewerId());
    }

    @Test
    @DisplayName("F-1 DONE에 이유 · 직접 입력이 null로 와도 200이다")
    void respondConfirmation_doneWithExplicitNulls_returns200() throws Exception {
        mockMvc.perform(confirmationRequest(OWNER_ID,
                        "{\"answer\": \"DONE\", \"notMetReason\": null, \"notMetDetail\": null}"))
                .andExpect(status().isOk());

        RespondConfirmationCommand command = (RespondConfirmationCommand) commandBus.sentCommands().get(0);
        assertEquals(OWNER_ID, command.memberId());
        assertEquals(ConfirmationAnswer.DONE, command.answer());
    }

    // ---- AC-2 · AC-3 못 만났어요 · 이유 남기기 ----

    @ParameterizedTest
    @EnumSource(com.ject.exchange.api.dto.enums.NotMetReason.class)
    @DisplayName("F-1 NOT_MET과 이유 6개 각각이면 200이고 같은 이름의 이유가 커맨드로 간다")
    void respondConfirmation_notMetEachReason_returns200(com.ject.exchange.api.dto.enums.NotMetReason reason)
            throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.NOT_MET,
                new ConfirmationSummary(OPENS_AT, AppointmentStatus.NOT_MET, false, ConfirmationAnswer.NOT_MET,
                        RESOLVED_AT)));

        mockMvc.perform(confirmationRequest(REQUESTER_ID,
                        "{\"answer\": \"NOT_MET\", \"notMetReason\": \"" + reason.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NOT_MET"))
                .andExpect(jsonPath("$.data.confirmation.result").value("NOT_MET"))
                .andExpect(jsonPath("$.data.confirmation.myAnswer").value("NOT_MET"));

        RespondConfirmationCommand command = (RespondConfirmationCommand) commandBus.sentCommands().get(0);
        assertEquals(ConfirmationAnswer.NOT_MET, command.answer());
        assertEquals(NotMetReason.valueOf(reason.name()), command.notMetReason());
        assertNull(command.notMetDetail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"   \"", "\"상대가 다른 물건을 가져왔어요\"", "null"})
    @DisplayName("F-1 OTHER의 직접 입력이 빈 문자열 · 공백 · 문장 · null이면 200이고 그대로 커맨드로 간다")
    void respondConfirmation_otherWithDetail_passesDetailAsIs(String detailJson) throws Exception {
        mockMvc.perform(confirmationRequest(REQUESTER_ID,
                        "{\"answer\": \"NOT_MET\", \"notMetReason\": \"OTHER\", \"notMetDetail\": " + detailJson + "}"))
                .andExpect(status().isOk());

        RespondConfirmationCommand command = (RespondConfirmationCommand) commandBus.sentCommands().get(0);
        assertEquals(NotMetReason.OTHER, command.notMetReason());
        String expected = detailJson.equals("null") ? null : detailJson.substring(1, detailJson.length() - 1);
        assertEquals(expected, command.notMetDetail());
    }

    @Test
    @DisplayName("F-1 OTHER의 직접 입력 100자면 200이다")
    void respondConfirmation_otherWith100Chars_returns200() throws Exception {
        String detail = "가".repeat(100);

        mockMvc.perform(confirmationRequest(REQUESTER_ID,
                        "{\"answer\": \"NOT_MET\", \"notMetReason\": \"OTHER\", \"notMetDetail\": \"" + detail + "\"}"))
                .andExpect(status().isOk());

        RespondConfirmationCommand command = (RespondConfirmationCommand) commandBus.sentCommands().get(0);
        assertEquals(detail, command.notMetDetail());
    }

    // ---- AC-10 본문 형식 ----

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"answer\": null}", "{\"notMetReason\": \"NO_SHOW\"}"})
    @DisplayName("F-1 answer가 없거나 null이면 400 COMMON_001이고 커맨드를 보내지 않는다")
    void respondConfirmation_missingAnswer_returnsCommon001(String body) throws Exception {
        assertValidationFailure(body, "answer");
    }

    @Test
    @DisplayName("F-1 직접 입력 101자면 400 COMMON_001이고 커맨드를 보내지 않는다")
    void respondConfirmation_detail101Chars_returnsCommon001() throws Exception {
        String detail = "가".repeat(101);

        assertValidationFailure("{\"answer\": \"NOT_MET\", \"notMetReason\": \"OTHER\", \"notMetDetail\": \""
                + detail + "\"}", "notMetDetail");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"answer\": \"MAYBE\"}",
            "{\"answer\": \"done\"}",
            "{\"answer\": \"NOT_MET\", \"notMetReason\": \"LATE\"}",
            "not-json"})
    @DisplayName("F-1 정의되지 않은 answer · notMetReason 값이나 읽을 수 없는 본문이면 400 COMMON_002")
    void respondConfirmation_unreadableBody_returnsCommon002(String body) throws Exception {
        mockMvc.perform(confirmationRequest(REQUESTER_ID, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_002"));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    @Test
    @DisplayName("F-1 본문이 없으면 400 COMMON_002이고 커맨드를 보내지 않는다")
    void respondConfirmation_withoutBody_returnsCommon002() throws Exception {
        mockMvc.perform(post(CONFIRMATION_URL, APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_002"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    // ---- AC-11 답과 이유가 맞지 않음 ----

    @Test
    @DisplayName("F-1 NOT_MET인데 이유가 없으면 400 COMMON_001 · notMetReason 필드 오류다")
    void respondConfirmation_notMetWithoutReason_returnsCommon001() throws Exception {
        assertValidationFailure("{\"answer\": \"NOT_MET\"}", "notMetReason");
    }

    @Test
    @DisplayName("F-1 NOT_MET인데 이유가 null이면 400 COMMON_001 · notMetReason 필드 오류다")
    void respondConfirmation_notMetWithNullReason_returnsCommon001() throws Exception {
        assertValidationFailure("{\"answer\": \"NOT_MET\", \"notMetReason\": null}", "notMetReason");
    }

    @Test
    @DisplayName("F-1 DONE인데 이유가 있으면 400 COMMON_001 · notMetReason 필드 오류다")
    void respondConfirmation_doneWithReason_returnsCommon001() throws Exception {
        assertValidationFailure("{\"answer\": \"DONE\", \"notMetReason\": \"NO_SHOW\"}", "notMetReason");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"메모\"", "\"\""})
    @DisplayName("F-1 DONE인데 직접 입력이 있으면 400 COMMON_001 · notMetDetail 필드 오류다")
    void respondConfirmation_doneWithDetail_returnsCommon001(String detailJson) throws Exception {
        assertValidationFailure("{\"answer\": \"DONE\", \"notMetDetail\": " + detailJson + "}", "notMetDetail");
    }

    @ParameterizedTest
    @EnumSource(value = com.ject.exchange.api.dto.enums.NotMetReason.class, names = "OTHER",
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("F-1 OTHER가 아닌 이유에 직접 입력이 있으면 400 COMMON_001 · notMetDetail 필드 오류다")
    void respondConfirmation_nonOtherWithDetail_returnsCommon001(com.ject.exchange.api.dto.enums.NotMetReason reason)
            throws Exception {
        assertValidationFailure("{\"answer\": \"NOT_MET\", \"notMetReason\": \"" + reason.name()
                + "\", \"notMetDetail\": \"메모\"}", "notMetDetail");
    }

    @Test
    @DisplayName("F-1 OTHER가 아닌 이유에 빈 문자열 직접 입력도 400 COMMON_001이다")
    void respondConfirmation_nonOtherWithEmptyDetail_returnsCommon001() throws Exception {
        assertValidationFailure("{\"answer\": \"NOT_MET\", \"notMetReason\": \"NO_SHOW\", \"notMetDetail\": \"\"}",
                "notMetDetail");
    }

    // ---- AC-12 인증 · 없는 약속 · 참여자 아님 ----

    @Test
    @DisplayName("F-1 로그인 회원 id가 없으면 401 AUTH_001이고 커맨드를 보내지 않는다")
    void respondConfirmation_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(post(CONFIRMATION_URL, APPOINTMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DONE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("F-1 로그인 회원 id가 없고 본문도 틀리면 401 AUTH_001이 먼저다")
    void respondConfirmation_withoutLoginMemberAndInvalidBody_returnsAuth001() throws Exception {
        mockMvc.perform(post(CONFIRMATION_URL, APPOINTMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\": \"NOT_MET\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("F-1 없는 약속이면 404 EXCHANGE_101이고 조회하지 않는다")
    void respondConfirmation_missing_returnsExchange101() throws Exception {
        assertFailure(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, 404);
    }

    @Test
    @DisplayName("F-1 참여자가 아니면 403 EXCHANGE_002")
    void respondConfirmation_thirdParty_returnsExchange002() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, 403);
    }

    // ---- 처리 실패 응답 (AC-4 ~ AC-8 · AC-13 · AC-14) ----

    @Test
    @DisplayName("F-1 결과가 이미 정해졌으면 409 EXCHANGE_303")
    void respondConfirmation_alreadyDecided_returnsExchange303() throws Exception {
        assertFailure(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, 409);
    }

    @Test
    @DisplayName("F-1 이미 답했으면 409 EXCHANGE_302")
    void respondConfirmation_alreadyAnswered_returnsExchange302() throws Exception {
        assertFailure(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED, 409);
    }

    @Test
    @DisplayName("F-1 열리기 전이면 409 EXCHANGE_301")
    void respondConfirmation_notOpen_returnsExchange301() throws Exception {
        assertFailure(ExchangeErrorCode.CONFIRMATION_NOT_OPEN, 409);
    }

    @Test
    @DisplayName("F-1 확정이 아닌 약속이면 409 EXCHANGE_109")
    void respondConfirmation_notConfirmed_returnsExchange109() throws Exception {
        assertFailure(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, 409);
    }

    @Test
    @DisplayName("F-1 종료된 교환이면 409 EXCHANGE_010")
    void respondConfirmation_exchangeClosed_returnsExchange010() throws Exception {
        assertFailure(ExchangeErrorCode.EXCHANGE_CLOSED, 409);
    }

    // ---- AC-15 응답의 confirmation ----

    @Test
    @DisplayName("F-1 B 기준 조회 결과에 내 응답이 없으면 myAnswer는 비어 있고 result는 COMPLETED다")
    void respondConfirmation_viewerWithoutAnswer_myAnswerAbsent() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.COMPLETED,
                new ConfirmationSummary(OPENS_AT, AppointmentStatus.COMPLETED, false, null, RESOLVED_AT)));

        mockMvc.perform(confirmationRequest(OWNER_ID, DONE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmation.result").value("COMPLETED"))
                .andExpect(jsonPath("$.data.confirmation.myAnswer").doesNotExist());
    }

    @Test
    @DisplayName("A-2 CONFIRMED이고 열리기 전이면 confirmation의 시각 · 결과 · 내 응답은 비어 있고 autoCompleted false다")
    void getAppointment_confirmedBeforeOpen_confirmationEmpty() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.CONFIRMED,
                new ConfirmationSummary(null, null, false, null, null)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/appointments/{appointmentId}", APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmation").exists())
                .andExpect(jsonPath("$.data.confirmation.askedAt").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation.result").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation.autoCompleted").value(false))
                .andExpect(jsonPath("$.data.confirmation.myAnswer").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation.resolvedAt").doesNotExist());
    }

    @Test
    @DisplayName("A-2 CONFIRMED이고 열린 뒤면 askedAt만 채워진다")
    void getAppointment_confirmedAfterOpen_askedAtOnly() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.CONFIRMED,
                new ConfirmationSummary(OPENS_AT, null, false, null, null)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/appointments/{appointmentId}", APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmation.askedAt").value("2026-11-23T15:30:00+09:00"))
                .andExpect(jsonPath("$.data.confirmation.result").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation.resolvedAt").doesNotExist());
    }

    @Test
    @DisplayName("A-2 조회 결과의 confirmation이 null이면 응답 confirmation도 비어 있다")
    void getAppointment_summaryNull_confirmationAbsent() throws Exception {
        queryBus.willReturnAppointment(result(AppointmentStatus.PROPOSED, null));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/appointments/{appointmentId}", APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmation").doesNotExist());
    }

    // ---- helpers ----

    private void assertValidationFailure(String body, String field) throws Exception {
        mockMvc.perform(confirmationRequest(REQUESTER_ID, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    private void assertFailure(ExchangeErrorCode errorCode, int httpStatus) throws Exception {
        commandBus.onRespondConfirmation(command -> {
            throw new ExchangeException(errorCode);
        });

        mockMvc.perform(confirmationRequest(REQUESTER_ID, DONE_BODY))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    private static MockHttpServletRequestBuilder confirmationRequest(Long memberId, String body) {
        return post(CONFIRMATION_URL, APPOINTMENT_ID)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static AppointmentResult result(AppointmentStatus status, ConfirmationSummary confirmation) {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", null)),
                null, 30, false, status == AppointmentStatus.PROPOSED ? null : CONFIRMED_AT, CREATED_AT,
                confirmation);
    }
}
