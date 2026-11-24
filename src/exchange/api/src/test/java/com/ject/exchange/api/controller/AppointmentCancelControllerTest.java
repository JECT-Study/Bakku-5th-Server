package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.CancelAppointmentCommand;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.port.PostStatusRejectedException;
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
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A-8 약속 취소 요청 · 응답 형식 · 에러 코드 (#113).
 */
class AppointmentCancelControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final String CANCEL_URL = "/api/v1/appointments/{appointmentId}/cancel";
    private static final String DETAIL_URL = "/api/v1/appointments/{appointmentId}";
    private static final String CURRENT_URL = "/api/v1/exchanges/{exchangeId}/appointments/current";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    /** 제안자 A */
    private static final Long PROPOSER_ID = 100L;
    /** 받는 사람 B */
    private static final Long RECEIVER_ID = 200L;
    private static final Long POST_P = 10L;
    private static final Long ITEM_31 = 31L;
    /** 2026-11-23 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-22 17:00 (+09:00) */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    /** 2026-11-22 15:00 (+09:00) */
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final String THUMBNAIL = "post/501/1.jpg";

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        commandBus.onCancelAppointment(command -> APPOINTMENT_ID);
        queryBus.willReturnAppointment(canceledResult(PROPOSER_ID, 30));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AppointmentController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- AC-1 제안자가 취소 ----

    @Test
    @DisplayName("A-8 제안자가 본문 없이 취소하면 200 · CANCELED · canceledByUserId 제안자다")
    void cancelAppointment_byProposer_returns200Canceled() throws Exception {
        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.data.exchangeId").value(EXCHANGE_ID))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.canceledByUserId").value(PROPOSER_ID))
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-11-22T17:00:00+09:00"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(30));
    }

    @Test
    @DisplayName("A-8 경로 id와 로그인 회원이 커맨드로 가고 반환 id와 취소한 사람으로 조회한다")
    void cancelAppointment_sendsCommandThenQueryAsCanceler() throws Exception {
        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isOk());

        assertEquals(1, commandBus.sentCommands().size());
        CancelAppointmentCommand command = assertInstanceOf(CancelAppointmentCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(APPOINTMENT_ID, command.appointmentId());
        assertEquals(PROPOSER_ID, command.cancelerId());

        assertEquals(1, queryBus.sentQueries().size());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(PROPOSER_ID, query.viewerId());
    }

    // ---- AC-2 받은 사람이 취소 ----

    @Test
    @DisplayName("A-8 받는 사람이 취소하면 200 · canceledByUserId 받는 사람이고 받는 사람 기준으로 조회한다")
    void cancelAppointment_byReceiver_returns200CanceledByReceiver() throws Exception {
        queryBus.willReturnAppointment(canceledResult(RECEIVER_ID, 60));

        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, RECEIVER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.canceledByUserId").value(RECEIVER_ID))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(60));

        CancelAppointmentCommand command = assertInstanceOf(CancelAppointmentCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(RECEIVER_ID, command.cancelerId());
        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(RECEIVER_ID, query.viewerId());
    }

    // ---- AC-4 응답에 임박 여부 없음 ----

    @Test
    @DisplayName("A-8 응답에는 임박 여부를 넣지 않는다")
    void cancelAppointment_responseHasNoImminentFlag() throws Exception {
        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelImminent").doesNotExist())
                .andExpect(jsonPath("$.data.imminent").doesNotExist())
                .andExpect(jsonPath("$.data.pendingCancellation").doesNotExist());
    }

    // ---- 처리 실패 응답 (AC-5 ~ AC-9 · AC-11) ----

    @Test
    @DisplayName("A-8 참여자가 아니면 403 EXCHANGE_002이고 조회하지 않는다")
    void cancelAppointment_thirdParty_returnsExchange002() throws Exception {
        assertFailure(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, 403);
    }

    @Test
    @DisplayName("A-8 없는 약속이면 404 EXCHANGE_101이고 조회하지 않는다")
    void cancelAppointment_missing_returnsExchange101() throws Exception {
        assertFailure(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, 404);
    }

    @Test
    @DisplayName("A-8 확정이 아닌 약속이면 409 EXCHANGE_109이고 조회하지 않는다")
    void cancelAppointment_notConfirmed_returnsExchange109() throws Exception {
        assertFailure(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, 409);
    }

    @Test
    @DisplayName("A-8 교환 확인이 열린 뒤면 409 EXCHANGE_207과 안내 문구다")
    void cancelAppointment_afterConfirmationOpen_returnsExchange207() throws Exception {
        commandBus.onCancelAppointment(command -> {
            throw new ExchangeException(ExchangeErrorCode.CANCEL_AFTER_CONFIRMATION_OPEN);
        });

        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_207"))
                .andExpect(jsonPath("$.message").value("교환 확인이 시작되어 약속을 취소할 수 없습니다."));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    @Test
    @DisplayName("A-8 교환이 종료됐으면 409 EXCHANGE_010이고 조회하지 않는다")
    void cancelAppointment_exchangeClosed_returnsExchange010() throws Exception {
        assertFailure(ExchangeErrorCode.EXCHANGE_CLOSED, 409);
    }

    @Test
    @DisplayName("A-8 post BC가 release를 거부하면 500 INTERNAL_001이고 조회하지 않는다")
    void cancelAppointment_postStatusRejected_returnsInternal001() throws Exception {
        commandBus.onCancelAppointment(command -> {
            throw new PostStatusRejectedException(POST_P);
        });

        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_001"));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    @Test
    @DisplayName("A-8 동시 취소에서 늦은 쪽은 409 EXCHANGE_109다")
    void cancelAppointment_lateConcurrentCancel_returnsExchange109() throws Exception {
        commandBus.onCancelAppointment(command -> {
            if (command.cancelerId().equals(PROPOSER_ID)) {
                return APPOINTMENT_ID;
            }
            throw new ExchangeException(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS);
        });

        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isOk());
        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, RECEIVER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_109"));
    }

    // ---- AC-13 취소된 약속 조회 ----

    @Test
    @DisplayName("A-2 취소된 약속 상세는 200 · CANCELED · canceledByUserId 취소한 사람이다")
    void getAppointment_canceled_returnsCanceledBy() throws Exception {
        queryBus.willReturnAppointment(canceledResult(PROPOSER_ID, 60));

        mockMvc.perform(get(DETAIL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, RECEIVER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.canceledByUserId").value(PROPOSER_ID))
                .andExpect(jsonPath("$.data.cancelImminent").doesNotExist());
    }

    @Test
    @DisplayName("A-2 취소되지 않은 약속 상세는 canceledByUserId가 null이다")
    void getAppointment_notCanceled_hasNullCanceledBy() throws Exception {
        queryBus.willReturnAppointment(confirmedResult());

        mockMvc.perform(get(DETAIL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.canceledByUserId").doesNotExist());
    }

    @Test
    @DisplayName("A-3 취소 뒤 유효한 약속이 없으면 200 · data null이다")
    void getCurrentAppointment_afterCancel_returnsNullData() throws Exception {
        queryBus.willReturnCurrentAppointment(Optional.empty());

        mockMvc.perform(get(CURRENT_URL, EXCHANGE_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$", hasEntry(equalTo("data"), nullValue())));
    }

    // ---- AC-14 인증 없음 ----

    @Test
    @DisplayName("A-8 로그인 회원 id가 없으면 401 AUTH_001이고 커맨드를 보내지 않는다")
    void cancelAppointment_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    // ---- helpers ----

    private void assertFailure(ExchangeErrorCode errorCode, int httpStatus) throws Exception {
        commandBus.onCancelAppointment(command -> {
            throw new ExchangeException(errorCode);
        });

        mockMvc.perform(post(CANCEL_URL, APPOINTMENT_ID).requestAttr(LoginMember.ATTRIBUTE, PROPOSER_ID))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    /** 취소된 약속의 조회 결과 (보는 사람의 알림 값만 다르다) */
    private static AppointmentResult canceledResult(Long canceledByUserId, Integer myReminderOffsetMinutes) {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID,
                AppointmentStatus.CANCELED, AppointmentTimeType.SCHEDULED, SCHEDULED_AT, place(),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, myReminderOffsetMinutes, false, CONFIRMED_AT, CREATED_AT, null, canceledByUserId);
    }

    private static AppointmentResult confirmedResult() {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, PROPOSER_ID, RECEIVER_ID,
                AppointmentStatus.CONFIRMED, AppointmentTimeType.SCHEDULED, SCHEDULED_AT, place(),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, 30, false, CONFIRMED_AT, CREATED_AT, null, null);
    }

    private static Place place() {
        return new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);
    }
}
