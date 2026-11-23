package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import com.ject.exchange.application.service.command.UpdateNewItemHandler;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class ExchangeControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final Instant NOW = Instant.parse("2026-11-20T05:30:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_ID = 31L;
    private static final Long MISSING_EXCHANGE_ID = 999L;
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String ORIGINAL_DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";
    private static final String UPDATE_URL = "/api/v1/exchanges/{exchangeId}/new-item";
    private static final String CREATE_URL = "/api/v1/exchanges";
    private static final String ONE_IMAGE_KEY = "[\"exchange/tmp/a.jpg\"]";

    private FakeExchangeRepository exchangeRepository;
    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        commandBus = new FakeCommandBus(new UpdateNewItemHandler(exchangeRepository));
        queryBus = new FakeQueryBus(exchangeRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ExchangeController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- 정상 흐름 ----

    @Test
    @DisplayName("E-3 요청자가 수정하면 200과 수정된 교환 상세를 반환한다")
    void updateNewItem_byRequester_returns200WithUpdatedItem() throws Exception {
        // given
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        // when & then
        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote("새 이름"), "true", quote("새 설명"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(EXCHANGE_ID))
                .andExpect(jsonPath("$.data.offerType").value("NEW"))
                .andExpect(jsonPath("$.data.offeredItems", hasSize(1)))
                .andExpect(jsonPath("$.data.offeredItems[0].id").value(ITEM_ID))
                .andExpect(jsonPath("$.data.offeredItems[0].name").value("새 이름"))
                .andExpect(jsonPath("$.data.offeredItems[0].opened").value(true))
                .andExpect(jsonPath("$.data.offeredItems[0].description").value("새 설명"));

        // then: 로그인 회원과 경로의 교환 id가 커맨드 · 쿼리에 그대로 실린다
        UpdateNewItemCommand command = assertInstanceOf(UpdateNewItemCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(EXCHANGE_ID, command.exchangeId());
        assertEquals(REQUESTER_ID, command.requesterId());
        assertEquals("새 이름", command.newItem().name());
        assertEquals(true, command.newItem().opened());
        assertEquals("새 설명", command.newItem().description());
        GetExchangeQuery query = assertInstanceOf(GetExchangeQuery.class, queryBus.sentQueries().get(0));
        assertEquals(EXCHANGE_ID, query.exchangeId());
        assertEquals(REQUESTER_ID, query.viewerId());
    }

    @Test
    @DisplayName("E-3 수정 후 E-2로 다시 조회하면 같은 값이 저장돼 있다")
    void updateNewItem_thenGetExchange_returnsSavedValues() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote("새 이름"), "true", quote("새 설명"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/exchanges/{exchangeId}", EXCHANGE_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].id").value(ITEM_ID))
                .andExpect(jsonPath("$.data.offeredItems[0].name").value("새 이름"))
                .andExpect(jsonPath("$.data.offeredItems[0].opened").value(true))
                .andExpect(jsonPath("$.data.offeredItems[0].description").value("새 설명"));
    }

    @Test
    @DisplayName("E-3 설명 없이 수정하면 200이고 아이템 설명은 null이다")
    void updateNewItem_withoutDescription_returns200WithNullDescription() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote("새 이름"), "true", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].description").doesNotExist());

        UpdateNewItemCommand command = (UpdateNewItemCommand) commandBus.sentCommands().get(0);
        assertNull(command.newItem().description());
    }

    @Test
    @DisplayName("E-3 설명 1,000자로 수정하면 200이고 설명 길이는 1,000이다")
    void updateNewItem_thousandCharacterDescription_returns200() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        String thousandCharacters = "가".repeat(1000);

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote("새 이름"), "false", quote(thousandCharacters))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].description").value(thousandCharacters));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 40})
    @DisplayName("E-3 제품명 2자 · 40자로 수정하면 200이고 제품명이 입력과 같다")
    void updateNewItem_boundaryNameLength_returns200(int length) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        String name = "가".repeat(length);

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote(name), "false", quote(ORIGINAL_DESCRIPTION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].name").value(name));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    @DisplayName("E-3 사진 키가 1장 · 5장이면 200")
    void updateNewItem_boundaryImageKeyCount_returns200(int count) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(imageKeys(count), quote("새 이름"), "false", null)))
                .andExpect(status().isOk());
    }

    // ---- 존재 · 권한 · 상태 실패 ----

    @Test
    @DisplayName("E-3 없는 교환이면 404 EXCHANGE_001")
    void updateNewItem_missingExchange_returnsExchange001() throws Exception {
        mockMvc.perform(updateRequest(MISSING_EXCHANGE_ID, REQUESTER_ID, validBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_001"));
    }

    @Test
    @DisplayName("E-3 게시자가 수정하면 403 EXCHANGE_012이고 아이템은 바뀌지 않는다")
    void updateNewItem_byOwner_returnsExchange012() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, OWNER_ID, validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_012"));

        assertStoredItemUnchanged();
    }

    @Test
    @DisplayName("E-3 교환 참여자가 아닌 회원이 수정하면 403 EXCHANGE_012이고 아이템은 바뀌지 않는다")
    void updateNewItem_byThirdParty_returnsExchange012() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, OTHER_MEMBER_ID, validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXCHANGE_012"));

        assertStoredItemUnchanged();
    }

    @Test
    @DisplayName("E-3 POST 방식 교환이면 409 EXCHANGE_013이고 아이템은 바뀌지 않는다")
    void updateNewItem_postOffer_returnsExchange013() throws Exception {
        exchangeRepository.save(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID, validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_013"));

        Exchange stored = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(OfferType.POST, stored.getOfferType());
        assertEquals(501L, stored.getOfferedItems().get(0).getSourcePostId());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("E-3 종료된(COMPLETED · CLOSED · LEFT) 교환이면 409 EXCHANGE_010이고 아이템은 바뀌지 않는다")
    void updateNewItem_notActive_returnsExchange010(ExchangeStatus status) throws Exception {
        exchangeRepository.save(newItemExchange(status));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID, validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_010"));

        assertStoredItemUnchanged();
    }

    // ---- 요청 검증 실패 ----

    static Stream<String> invalidNames() {
        return Stream.of(null, quote(""), quote("   "), quote("가"), quote("가".repeat(41)), "null");
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    @DisplayName("E-3 제품명이 누락 · 빈 문자열 · 공백만 · 1자 · 41자 · null이면 400 COMMON_001, field=name")
    void updateNewItem_invalidName_returnsCommon001(String nameJson) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, nameJson, "true", quote("새 설명"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertStoredItemUnchanged();
    }

    @Test
    @DisplayName("E-3 설명이 1,001자면 400 COMMON_001, field=description")
    void updateNewItem_tooLongDescription_returnsCommon001() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote("새 이름"), "true", quote("가".repeat(1001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("description")));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertStoredItemUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"omit", "null"})
    @DisplayName("E-3 상품 상태(opened)가 누락 · null이면 400 COMMON_001, field=opened")
    void updateNewItem_missingOpened_returnsCommon001(String openedCase) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));
        String openedJson = openedCase.equals("omit") ? null : "null";

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(ONE_IMAGE_KEY, quote("새 이름"), openedJson, quote("새 설명"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("opened")));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertStoredItemUnchanged();
    }

    static Stream<String> invalidImageKeys() {
        return Stream.of(null, "null", "[]", imageKeys(6), "[\"\"]", "[\"exchange/tmp/a.jpg\", \"  \"]");
    }

    @ParameterizedTest
    @MethodSource("invalidImageKeys")
    @DisplayName("E-3 사진 키가 누락 · 0개 · 6개 · 빈 문자열 원소 포함이면 400 COMMON_001, field가 imageKeys로 시작")
    void updateNewItem_invalidImageKeys_returnsCommon001(String imageKeysJson) throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(updateRequest(EXCHANGE_ID, REQUESTER_ID,
                        body(imageKeysJson, quote("새 이름"), "true", quote("새 설명"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem(startsWith("imageKeys"))));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertStoredItemUnchanged();
    }

    // ---- 인증 ----

    @Test
    @DisplayName("E-3 로그인 회원 id가 없으면 401 AUTH_001")
    void updateNewItem_withoutLoginMember_returnsAuth001() throws Exception {
        exchangeRepository.save(newItemExchange(ExchangeStatus.ACTIVE));

        mockMvc.perform(put(UPDATE_URL, EXCHANGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertStoredItemUnchanged();
    }

    // ---- E-1 상품 상태 필수 (후속 작업) ----

    @ParameterizedTest
    @ValueSource(strings = {"omit", "null"})
    @DisplayName("E-1 NEW 방식에서 newItem.opened가 누락 · null이면 400 COMMON_001, field=newItem.opened이고 교환이 저장되지 않는다")
    void createExchange_newItemMissingOpened_returnsCommon001(String openedCase) throws Exception {
        String openedJson = openedCase.equals("omit") ? null : "null";
        String newItemJson = body(ONE_IMAGE_KEY, quote(ORIGINAL_NAME), openedJson, quote(ORIGINAL_DESCRIPTION));
        String requestJson = "{\"postId\": " + POST_ID + ", \"offerType\": \"NEW\", \"newItem\": " + newItemJson + "}";

        mockMvc.perform(post(CREATE_URL)
                        .requestAttr(LoginMember.ATTRIBUTE, REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("newItem.opened")));

        // then: 검증 실패라 생성 커맨드가 버스로 가지 않고, 저장소에도 교환이 없다
        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(exchangeRepository.findById(EXCHANGE_ID).isEmpty());
    }

    // ---- helpers ----

    private static MockHttpServletRequestBuilder updateRequest(Long exchangeId, Long memberId, String body) {
        return put(UPDATE_URL, exchangeId)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String validBody() {
        return body(ONE_IMAGE_KEY, quote("새 이름"), "true", quote("새 설명"));
    }

    /** 각 인자는 JSON 값 그대로다. Java null이면 해당 필드를 본문에서 뺀다. */
    private static String body(String imageKeysJson, String nameJson, String openedJson, String descriptionJson) {
        List<String> fields = new ArrayList<>();
        if (imageKeysJson != null) {
            fields.add("\"imageKeys\": " + imageKeysJson);
        }
        if (nameJson != null) {
            fields.add("\"name\": " + nameJson);
        }
        if (openedJson != null) {
            fields.add("\"opened\": " + openedJson);
        }
        if (descriptionJson != null) {
            fields.add("\"description\": " + descriptionJson);
        }
        return "{" + String.join(", ", fields) + "}";
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    private static String imageKeys(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> quote("exchange/tmp/" + index + ".jpg"))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription(ORIGINAL_DESCRIPTION), 0)),
                status, completedAt, NOW, NOW);
    }

    private void assertStoredItemUnchanged() {
        Exchange stored = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(1, stored.getOfferedItems().size());
        OfferedItem item = stored.getOfferedItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        assertEquals(ORIGINAL_NAME, item.getName().value());
        assertEquals(false, item.getOpened());
        assertEquals(ORIGINAL_DESCRIPTION, item.getDescription().value());
    }
}
