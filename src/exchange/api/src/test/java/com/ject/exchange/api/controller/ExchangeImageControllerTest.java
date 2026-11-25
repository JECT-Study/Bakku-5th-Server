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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E-1 · E-2 · E-3가 새로 작성한 아이템 사진을 확정 · 저장하고 URL로 응답하는지 확인한다 (#108).
 * 버스는 실제 Handler로 넘기고, 글 · 회원 · 사진 · 저장소는 Fake로 만든다.
 */
class ExchangeImageControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");
    private static final String IMAGE_URL_PREFIX = "https://img.test/api/v1/images/";
    private static final Instant NOW = Instant.parse("2026-11-23T05:00:00Z");
    private static final String CREATE_URL = "/api/v1/exchanges";
    private static final String DETAIL_URL = "/api/v1/exchanges/{exchangeId}";
    private static final String UPDATE_URL = "/api/v1/exchanges/{exchangeId}/new-item";

    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    /** B의 글 P */
    private static final Long POST_P = 10L;
    /** A의 글 Q1 */
    private static final Long POST_Q1 = 20L;
    private static final Long EXCHANGE_X = 1L;
    private static final Long ITEM_ID = 31L;
    private static final String K1 = "exchange/tmp/101/0b1c2d3e-0001.jpg";
    private static final String K2 = "exchange/tmp/101/0b1c2d3e-0002.jpg";
    private static final String K3 = "exchange/tmp/101/0b1c2d3e-0003.jpg";
    private static final String K1_FINAL = "exchange/1/0b1c2d3e-0001.jpg";
    private static final String K2_FINAL = "exchange/1/0b1c2d3e-0002.jpg";
    private static final String K3_FINAL = "exchange/1/0b1c2d3e-0003.jpg";
    private static final String OTHER_MEMBER_TMP_KEY = "exchange/tmp/103/0b1c2d3e-0009.jpg";
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String ORIGINAL_DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";

    private FakeExchangeRepository exchangeRepository;
    private FakeImagePort imagePort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        imagePort = new FakeImagePort();
        FakePostPort postPort = new FakePostPort();
        FakeUserPort userPort = new FakeUserPort();
        FakeCommandBus commandBus = new FakeCommandBus(new UpdateNewItemHandler(exchangeRepository, new FakeAppointmentRepository(exchangeRepository, NOW),
                new FakePostLockRepository(), imagePort),
                new CreateExchangeHandler(exchangeRepository, postPort, userPort, imagePort));
        FakeQueryBus queryBus = new FakeQueryBus(new GetExchangeHandler(exchangeRepository,
                new FakeAppointmentRepository(exchangeRepository, NOW), postPort, userPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")))));
        mockMvc = MockMvcBuilders.standaloneSetup(new ExchangeController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        postPort.register(new PostInfo(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_Q1, "Q1", "post/20/1.jpg", MEMBER_A, PostInfo.Status.AVAILABLE));
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 0));
        userPort.register(new UserInfo(MEMBER_B, "하치", 0));
    }

    // ---- AC-1 · AC-8 E-1 ----

    @Test
    @DisplayName("E-1 NEW 방식 사진 [K1, K2]면 201이고 imageUrls는 정식 key URL [K1, K2] 순서다")
    void createExchange_newItem_returnsImageUrlsInOrder() throws Exception {
        // when & then
        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P, List.of(K1, K2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls", contains(
                        IMAGE_URL_PREFIX + "exchange/1000/0b1c2d3e-0001.jpg",
                        IMAGE_URL_PREFIX + "exchange/1000/0b1c2d3e-0002.jpg")));

        // then: 저장된 교환의 사진도 같은 순서의 정식 key다
        List<Exchange> saved = exchangeRepository.findAll();
        assertEquals(1, saved.size());
        Long exchangeId = saved.get(0).getId();
        assertEquals(List.of(FakeImagePort.toFinalKey(exchangeId, K1), FakeImagePort.toFinalKey(exchangeId, K2)),
                saved.get(0).getOfferedItems().get(0).getImageKeys());
    }

    // ---- AC-2 · AC-11 ----

    @Test
    @DisplayName("E-1 두 번째 사진이 거부되면 400 EXCHANGE_015이고 메시지에 2번째라고 담는다")
    void createExchange_secondImageRejected_returnsExchange015() throws Exception {
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P, List.of(K1, OTHER_MEMBER_TMP_KEY))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_015"))
                .andExpect(jsonPath("$.message").value("2번째 이미지를 사용할 수 없습니다."));
    }

    // ---- AC-4 ----

    @Test
    @DisplayName("E-1 newItem.imageKeys에 같은 key가 두 번이면 400 COMMON_001, field=newItem.imageKeys이고 사진 확정을 부르지 않는다")
    void createExchange_duplicateImageKeys_returnsCommon001() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P, List.of(K1, K1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("newItem.imageKeys")));

        assertTrue(imagePort.calls().isEmpty());
        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 서로 다른 key 5장이면 중복 검증을 통과해 201이다")
    void createExchange_fiveDistinctImageKeys_returns201() throws Exception {
        List<String> keys = List.of(K1, K2, K3,
                "exchange/tmp/101/0b1c2d3e-0004.jpg", "exchange/tmp/101/0b1c2d3e-0005.jpg");

        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P, keys)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls", hasSize(5)));
    }

    @Test
    @DisplayName("E-3 imageKeys에 같은 key가 두 번이면 400 COMMON_001, field=imageKeys이고 사진 확정을 부르지 않는다")
    void updateNewItem_duplicateImageKeys_returnsCommon001() throws Exception {
        exchangeRepository.save(newItemExchange());

        mockMvc.perform(updateRequest(MEMBER_A, updateBody(List.of(K2_FINAL, K2_FINAL))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("imageKeys")));

        assertTrue(imagePort.calls().isEmpty());
        assertEquals(List.of(K1_FINAL, K2_FINAL), storedImageKeys());
    }

    // ---- AC-5 · AC-8 E-3 ----

    @Test
    @DisplayName("E-3 사진을 [K2 정식, K3]로 바꾸면 200이고 imageUrls는 [K2 정식, K3 정식] 순서다")
    void updateNewItem_replaceImages_returnsNewOrder() throws Exception {
        exchangeRepository.save(newItemExchange());

        mockMvc.perform(updateRequest(MEMBER_A, updateBody(List.of(K2_FINAL, K3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls", contains(
                        IMAGE_URL_PREFIX + K2_FINAL, IMAGE_URL_PREFIX + K3_FINAL)));

        assertEquals(List.of(K2_FINAL, K3_FINAL), storedImageKeys());
    }

    // ---- AC-6 ----

    @Test
    @DisplayName("E-3 쓸 수 없는 사진이면 400 EXCHANGE_015이고 E-2로 보면 제품명 · 사진이 이전 그대로다")
    void updateNewItem_imageRejected_returnsExchange015AndKeepsItem() throws Exception {
        // given
        exchangeRepository.save(newItemExchange());
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        // when & then
        mockMvc.perform(updateRequest(MEMBER_A, updateBody(List.of(K2_FINAL, OTHER_MEMBER_TMP_KEY))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXCHANGE_015"))
                .andExpect(jsonPath("$.message").value("2번째 이미지를 사용할 수 없습니다."));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].name").value(ORIGINAL_NAME))
                .andExpect(jsonPath("$.data.offeredItems[0].opened").value(false))
                .andExpect(jsonPath("$.data.offeredItems[0].description").value(ORIGINAL_DESCRIPTION))
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls", contains(
                        IMAGE_URL_PREFIX + K1_FINAL, IMAGE_URL_PREFIX + K2_FINAL)));
    }

    // ---- AC-7 ----

    @Test
    @DisplayName("E-3 게시자가 쓸 수 없는 사진으로 수정하면 403 EXCHANGE_012이고 사진 확정을 부르지 않는다")
    void updateNewItem_byOwnerWithRejectedImage_returnsExchange012() throws Exception {
        exchangeRepository.save(newItemExchange());
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        mockMvc.perform(updateRequest(MEMBER_B, updateBody(List.of(OTHER_MEMBER_TMP_KEY))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXCHANGE_012"));

        assertTrue(imagePort.calls().isEmpty());
    }

    // ---- AC-8 E-2 ----

    @ParameterizedTest
    @ValueSource(longs = {101L, 102L})
    @DisplayName("E-2 요청자 A · 게시자 B가 보면 imageUrls는 저장 순서대로 ImageUrlResolver URL이다")
    void getExchange_newItem_returnsImageUrlsInStoredOrder(Long viewerId) throws Exception {
        exchangeRepository.save(newItemExchange());

        mockMvc.perform(detailRequest(viewerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls", contains(
                        IMAGE_URL_PREFIX + K1_FINAL, IMAGE_URL_PREFIX + K2_FINAL)));
    }

    // ---- AC-10 ----

    @Test
    @DisplayName("E-2 POST 방식 교환이면 첨부 아이템 imageUrls는 null이다")
    void getExchange_postOffer_hasNullImageUrls() throws Exception {
        exchangeRepository.save(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, POST_Q1, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].type").value("POST"))
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls").doesNotExist());
    }

    @Test
    @DisplayName("E-1 POST 방식 요청이면 201이고 imageUrls는 null이며 사진 확정을 부르지 않는다")
    void createExchange_postOffer_hasNullImageUrls() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, "{\"postId\": " + POST_P
                        + ", \"offerType\": \"POST\", \"offeredPostIds\": [" + POST_Q1 + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.offeredItems[0].imageUrls").doesNotExist());

        assertTrue(imagePort.calls().isEmpty());
    }

    // ---- helpers ----

    private List<String> storedImageKeys() {
        return exchangeRepository.findById(EXCHANGE_X).orElseThrow().getOfferedItems().get(0).getImageKeys();
    }

    /** AC-1의 교환: 사진 K1 · K2 정식 */
    private static Exchange newItemExchange() {
        return Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription(ORIGINAL_DESCRIPTION), 0, List.of(K1_FINAL, K2_FINAL))),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }

    private static MockHttpServletRequestBuilder createRequest(Long memberId, String body) {
        return post(CREATE_URL)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder updateRequest(Long memberId, String body) {
        return put(UPDATE_URL, EXCHANGE_X)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder detailRequest(Long memberId) {
        return get(DETAIL_URL, EXCHANGE_X).requestAttr(LoginMember.ATTRIBUTE, memberId);
    }

    private static String newItemBody(Long postId, List<String> imageKeys) {
        return "{\"postId\": " + postId + ", \"offerType\": \"NEW\", \"newItem\": "
                + newItemJson(imageKeys, ORIGINAL_NAME, false, ORIGINAL_DESCRIPTION) + "}";
    }

    private static String updateBody(List<String> imageKeys) {
        return newItemJson(imageKeys, "새 이름", true, "새 설명");
    }

    private static String newItemJson(List<String> imageKeys, String name, boolean opened, String description) {
        String keys = imageKeys.stream()
                .map(key -> "\"" + key + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        return "{\"imageKeys\": " + keys + ", \"name\": \"" + name + "\", \"opened\": " + opened
                + ", \"description\": \"" + description + "\"}";
    }
}
