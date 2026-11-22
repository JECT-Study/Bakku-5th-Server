package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.RejectAppointmentCommand;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class AppointmentRejectControllerTest {

    private static final String REJECT_URL = "/api/v1/appointments/{appointmentId}/reject";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_31 = 31L;
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");
    private static final String THUMBNAIL = "https://cdn.example.com/post/501/1.jpg";

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        commandBus.onRejectAppointment(command -> APPOINTMENT_ID);
        queryBus.willReturnAppointment(rejectedResult());
        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(commandBus, queryBus))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- 정상 흐름 (AC-1) ----

    @Test
    @DisplayName("A-5 받는 사람이 본문 없이 거절하면 200 · REJECTED · 확정 null · 만료 null · 내 알림 null이다")
    void rejectAppointment_byReceiver_returns200Rejected() throws Exception {
        mockMvc.perform(post(REJECT_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.exchangeId").value(EXCHANGE_ID))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-10-27T15:00:00+09:00"))
                .andExpect(jsonPath("$.data.confirmedAt").doesNotExist())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").doesNotExist());
    }

    @Test
    @DisplayName("A-5 경로 id와 로그인 회원이 커맨드로 가고 반환 id와 거절자로 조회한다")
    void rejectAppointment_sendsCommandThenQueryAsRejecter() throws Exception {
        mockMvc.perform(post(REJECT_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, OWNER_ID))
                .andExpect(status().isOk());

        assertEquals(1, commandBus.sentCommands().size());
        RejectAppointmentCommand command = assertInstanceOf(RejectAppointmentCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(OWNER_ID, command.rejecterId());

        assertEquals(1, queryBus.sentQueries().size());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(OWNER_ID, query.viewerId());
    }

    // ---- 인증 (AC-4) ----

    @Test
    @DisplayName("A-5 로그인 회원 id가 없으면 401 AUTH_001이고 커맨드를 보내지 않는다")
    void rejectAppointment_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(post(REJECT_URL, APPOINTMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    // ---- 처리 실패 응답 ----

    @Test
    @DisplayName("A-5 없는 약속이면 404 EXCHANGE_101이고 조회하지 않는다")
    void rejectAppointment_missing_returnsExchange101() throws Exception {
        assertFailure(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, 404);
    }

    @Test
    @DisplayName("A-5 참여자가 아니면 403 EXCHANGE_002")
    void rejectAppointment_thirdParty_returnsExchange002() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, 403);
    }

    @Test
    @DisplayName("A-5 제안자가 거절하면 403 EXCHANGE_104")
    void rejectAppointment_byProposer_returnsExchange104() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER, 403);
    }

    @Test
    @DisplayName("A-5 진행 중이 아니거나 만료된 제안이면 409 EXCHANGE_106")
    void rejectAppointment_proposalNotActive_returnsExchange106() throws Exception {
        assertFailure(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE, 409);
    }

    @Test
    @DisplayName("A-5 교환이 종료됐으면 409 EXCHANGE_010")
    void rejectAppointment_exchangeClosed_returnsExchange010() throws Exception {
        assertFailure(ExchangeErrorCode.EXCHANGE_CLOSED, 409);
    }

    // ---- helpers ----

    private void assertFailure(ExchangeErrorCode errorCode, int httpStatus) throws Exception {
        commandBus.onRejectAppointment(command -> {
            throw new ExchangeException(errorCode);
        });

        mockMvc.perform(post(REJECT_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, OWNER_ID))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    /** 거절자(받는 사람) 기준 거절된 약속 조회 결과 */
    private static AppointmentResult rejectedResult() {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.REJECTED,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, null, false, null, CREATED_AT);
    }
}
