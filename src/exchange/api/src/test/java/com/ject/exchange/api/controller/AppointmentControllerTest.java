package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class AppointmentControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final String PROPOSE_URL = "/api/v1/exchanges/{exchangeId}/appointments";
    private static final Long EXCHANGE_ID = 1L;
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_31 = 31L;
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");
    private static final String PLACE_NAME = "홍대입구역 3번 출구 앞 편의점";
    private static final String PLACE_ADDRESS = "서울 마포구 양화로 160";
    private static final String THUMBNAIL = "post/501/1.jpg";
    private static final String THUMBNAIL_URL = "https://img.test/api/v1/images/post/501/1.jpg";
    private static final String VALID_PLACE = place(quote(PLACE_NAME), quote(PLACE_ADDRESS), "37.5571", "126.9245");

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        commandBus.onProposeAppointment(command -> APPOINTMENT_ID);
        queryBus.willReturnAppointment(proposedResult());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AppointmentController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("A-1 유효한 시간 지정 제안이면 201과 약속 응답(만료 null · 내 알림 30 · pendingChange 없음)을 반환한다")
    void proposeAppointment_valid_returns201WithAppointment() throws Exception {
        mockMvc.perform(proposeRequest(REQUESTER_ID, validBody()))
                .andExpect(status().isCreated())
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
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(30))
                .andExpect(jsonPath("$.data.otherActiveProposalsOnPost").value(false))
                .andExpect(jsonPath("$.data.pendingChange").doesNotExist())
                .andExpect(jsonPath("$.data.canceledByUserId").doesNotExist())
                .andExpect(jsonPath("$.data.confirmation").doesNotExist())
                .andExpect(jsonPath("$.data.confirmedAt").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").value("2026-10-26T14:00:00+09:00"));
    }

    @Test
    @DisplayName("A-1 로그인 회원 · 경로 교환 id · 본문이 커맨드로 · 약속 id와 회원이 쿼리로 전달된다")
    void proposeAppointment_valid_sendsCommandAndQuery() throws Exception {
        mockMvc.perform(proposeRequest(REQUESTER_ID, validBody()))
                .andExpect(status().isCreated());

        ProposeAppointmentCommand command = assertInstanceOf(ProposeAppointmentCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(EXCHANGE_ID, command.exchangeId());
        assertEquals(REQUESTER_ID, command.proposerId());
        assertEquals(AppointmentTimeType.SCHEDULED, command.timeType());
        assertEquals(SCHEDULED_AT, command.scheduledAt());
        assertEquals(new Place(PLACE_NAME, PLACE_ADDRESS, 37.5571, 126.9245), command.place());
        assertEquals(List.of(ITEM_31), command.offeredItemIds());
        assertEquals(new ProposeAppointmentCommand(EXCHANGE_ID, REQUESTER_ID, AppointmentTimeType.SCHEDULED,
                SCHEDULED_AT, new Place(PLACE_NAME, PLACE_ADDRESS, 37.5571, 126.9245), List.of(ITEM_31)), command);

        GetAppointmentQuery query = assertInstanceOf(GetAppointmentQuery.class, queryBus.sentQueries().get(0));
        assertEquals(APPOINTMENT_ID, query.appointmentId());
        assertEquals(REQUESTER_ID, query.viewerId());
    }

    @Test
    @DisplayName("A-1 지금 바로 제안이면 커맨드의 시간 유형이 NOW이고 시각은 없다")
    void proposeAppointment_now_sendsNowCommand() throws Exception {
        String body = body(quote("NOW"), null, VALID_PLACE, null, "30");

        mockMvc.perform(proposeRequest(REQUESTER_ID, body))
                .andExpect(status().isCreated());

        ProposeAppointmentCommand command = (ProposeAppointmentCommand) commandBus.sentCommands().get(0);
        assertEquals(AppointmentTimeType.NOW, command.timeType());
        assertNull(command.scheduledAt());
        assertNull(command.offeredItemIds());
    }

    @Test
    @DisplayName("A-1 지금 바로 제안에 시각을 같이 보내도 201")
    void proposeAppointment_nowWithScheduledAt_returns201() throws Exception {
        String body = body(quote("NOW"), quote("2026-10-27T15:00:00+09:00"), VALID_PLACE, null, null);

        mockMvc.perform(proposeRequest(REQUESTER_ID, body))
                .andExpect(status().isCreated());

        ProposeAppointmentCommand command = (ProposeAppointmentCommand) commandBus.sentCommands().get(0);
        assertEquals(AppointmentTimeType.NOW, command.timeType());
    }

    static Stream<Arguments> boundaryBodies() {
        return Stream.of(
                Arguments.of("장소 이름 100자", body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"),
                        place(quote("가".repeat(100)), null, "37.5", "126.9"), null, "30")),
                Arguments.of("주소 255자", body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"),
                        place(quote(PLACE_NAME), quote("가".repeat(255)), "37.5", "126.9"), null, "30")),
                Arguments.of("위도 · 경도 최댓값", body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"),
                        place(quote(PLACE_NAME), null, "90", "180"), null, "30")),
                Arguments.of("위도 · 경도 최솟값", body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"),
                        place(quote(PLACE_NAME), null, "-90", "-180"), null, "30")),
                Arguments.of("아이템 생략", body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"),
                        VALID_PLACE, null, "30")),
                Arguments.of("아이템 2개", body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"),
                        VALID_PLACE, "[31, 32]", "30")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaryBodies")
    @DisplayName("A-1 입력 경계값(이름 100자 · 주소 255자 · 좌표 끝값 · 아이템 생략)이면 201")
    void proposeAppointment_boundaryInput_returns201(String caseName, String body) throws Exception {
        mockMvc.perform(proposeRequest(REQUESTER_ID, body))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("legacyReminders")
    @DisplayName("A-1 본문에 옛 알림 값(10 · 5 · 15 · 2000 · null · 없음)을 보내도 201이고 커맨드는 알림 없이 같다")
    void proposeAppointment_legacyReminder_ignored(String reminderJson) throws Exception {
        String body = body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"), VALID_PLACE, "[31]",
                reminderJson);

        mockMvc.perform(proposeRequest(REQUESTER_ID, body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").value(30));

        assertEquals(List.of(new ProposeAppointmentCommand(EXCHANGE_ID, REQUESTER_ID, AppointmentTimeType.SCHEDULED,
                SCHEDULED_AT, new Place(PLACE_NAME, PLACE_ADDRESS, 37.5571, 126.9245), List.of(ITEM_31))),
                commandBus.sentCommands());
    }

    static Stream<String> legacyReminders() {
        return Stream.of("10", "5", "15", "2000", "9", "1441", "null", null);
    }

    @Test
    @DisplayName("A-1 지금 바로 제안이면 201이고 응답 내 알림은 null이다")
    void proposeAppointment_now_returnsNullReminder() throws Exception {
        queryBus.willReturnAppointment(new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, AppointmentTimeType.NOW, null,
                new Place(PLACE_NAME, PLACE_ADDRESS, 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, null, false, null, CREATED_AT));

        mockMvc.perform(proposeRequest(REQUESTER_ID, body(quote("NOW"), null, VALID_PLACE, "[31]", "30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.timeType").value("NOW"))
                .andExpect(jsonPath("$.data.myReminderOffsetMinutes").doesNotExist())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
    }

    // ---- 요청 검증 실패 ----

    static Stream<Arguments> invalidBodies() {
        String time = quote("2026-10-27T15:00:00+09:00");
        String scheduled = quote("SCHEDULED");
        return Stream.of(
                Arguments.of("timeType 누락", body(null, time, VALID_PLACE, "[31]", "30"), "timeType"),
                Arguments.of("place 누락", body(scheduled, time, null, "[31]", "30"), "place"),
                Arguments.of("장소 이름 공백", body(scheduled, time,
                        place(quote("   "), null, "37.5", "126.9"), "[31]", "30"), "place.name"),
                Arguments.of("장소 이름 누락", body(scheduled, time,
                        place(null, null, "37.5", "126.9"), "[31]", "30"), "place.name"),
                Arguments.of("장소 이름 101자", body(scheduled, time,
                        place(quote("가".repeat(101)), null, "37.5", "126.9"), "[31]", "30"), "place.name"),
                Arguments.of("주소 256자", body(scheduled, time,
                        place(quote(PLACE_NAME), quote("가".repeat(256)), "37.5", "126.9"), "[31]", "30"),
                        "place.address"),
                Arguments.of("위도 90 초과", body(scheduled, time,
                        place(quote(PLACE_NAME), null, "90.0001", "126.9"), "[31]", "30"), "place.lat"),
                Arguments.of("위도 -90 미만", body(scheduled, time,
                        place(quote(PLACE_NAME), null, "-90.0001", "126.9"), "[31]", "30"), "place.lat"),
                Arguments.of("경도 180 초과", body(scheduled, time,
                        place(quote(PLACE_NAME), null, "37.5", "180.0001"), "[31]", "30"), "place.lng"),
                Arguments.of("경도 -180 미만", body(scheduled, time,
                        place(quote(PLACE_NAME), null, "37.5", "-180.0001"), "[31]", "30"), "place.lng"),
                Arguments.of("위도 누락", body(scheduled, time,
                        place(quote(PLACE_NAME), null, null, "126.9"), "[31]", "30"), "place.lat"),
                Arguments.of("경도 누락", body(scheduled, time,
                        place(quote(PLACE_NAME), null, "37.5", null), "[31]", "30"), "place.lng"),
                Arguments.of("아이템 빈 배열", body(scheduled, time, VALID_PLACE, "[]", "30"), "offeredItemIds"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    @DisplayName("A-1 입력 형식 위반이면 400 COMMON_001과 해당 field이고 커맨드를 보내지 않는다")
    void proposeAppointment_invalidInput_returnsCommon001(String caseName, String body, String field)
            throws Exception {
        mockMvc.perform(proposeRequest(REQUESTER_ID, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("A-1 아이템 목록에 null 원소가 있으면 400 COMMON_001이고 field가 offeredItemIds로 시작한다")
    void proposeAppointment_nullItemElement_returnsCommon001() throws Exception {
        String body = body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"), VALID_PLACE, "[31, null]", "30");

        mockMvc.perform(proposeRequest(REQUESTER_ID, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem(startsWith("offeredItemIds"))));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    // ---- 인증 ----

    @Test
    @DisplayName("A-1 로그인 회원 id가 없으면 401 AUTH_001이고 커맨드를 보내지 않는다")
    void proposeAppointment_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(post(PROPOSE_URL, EXCHANGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    // ---- Handler 예외 응답 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeErrorCode.class, names = {"EXCHANGE_NOT_FOUND", "NOT_EXCHANGE_PARTICIPANT",
            "EXCHANGE_CLOSED", "INVALID_SCHEDULED_AT", "APPOINTMENT_ALREADY_EXISTS", "POST_RESERVED_ELSEWHERE",
            "POST_NOT_REQUESTABLE", "INVALID_OFFERED_ITEMS", "ITEM_NOT_AVAILABLE"})
    @DisplayName("A-1 제안이 거부되면 에러 코드의 상태와 코드로 응답하고 약속을 조회하지 않는다")
    void proposeAppointment_handlerRejects_returnsErrorCode(ExchangeErrorCode errorCode) throws Exception {
        commandBus.onProposeAppointment(command -> {
            throw new ExchangeException(errorCode);
        });

        mockMvc.perform(proposeRequest(REQUESTER_ID, validBody()))
                .andExpect(status().is(errorCode.getStatus()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));

        assertEquals(1, commandBus.sentCommands().size());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    // ---- helpers ----

    private static MockHttpServletRequestBuilder proposeRequest(Long memberId, String body) {
        return post(PROPOSE_URL, EXCHANGE_ID)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String validBody() {
        return body(quote("SCHEDULED"), quote("2026-10-27T15:00:00+09:00"), VALID_PLACE, "[31]", "30");
    }

    /** 각 인자는 JSON 값 그대로다. Java null이면 해당 필드를 본문에서 뺀다. */
    private static String body(String timeTypeJson, String scheduledAtJson, String placeJson,
                               String offeredItemIdsJson, String legacyReminderJson) {
        List<String> fields = new ArrayList<>();
        addField(fields, "timeType", timeTypeJson);
        addField(fields, "scheduledAt", scheduledAtJson);
        addField(fields, "place", placeJson);
        addField(fields, "offeredItemIds", offeredItemIdsJson);
        // 옛 FE가 보내던 필드. 서버는 무시한다 (#117)
        addField(fields, "reminderOffsetMinutes", legacyReminderJson);
        return "{" + String.join(", ", fields) + "}";
    }

    /** 각 인자는 JSON 값 그대로다. Java null이면 해당 필드를 본문에서 뺀다. */
    private static String place(String nameJson, String addressJson, String latJson, String lngJson) {
        List<String> fields = new ArrayList<>();
        addField(fields, "name", nameJson);
        addField(fields, "address", addressJson);
        addField(fields, "lat", latJson);
        addField(fields, "lng", lngJson);
        return "{" + String.join(", ", fields) + "}";
    }

    private static void addField(List<String> fields, String name, String valueJson) {
        if (valueJson != null) {
            fields.add("\"" + name + "\": " + valueJson);
        }
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    private static AppointmentResult proposedResult() {
        return new AppointmentResult(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.PROPOSED,
                AppointmentTimeType.SCHEDULED, SCHEDULED_AT, new Place(PLACE_NAME, PLACE_ADDRESS, 37.5571, 126.9245),
                List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL)),
                null, 30, false, null, CREATED_AT);
    }
}
