package com.ject.exchange.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E-1 · E-2가 하드코딩 회원 대신 로그인 회원으로 요청자 · 조회자를 정하는지 확인한다.
 * 버스는 실제 Handler(CreateExchangeHandler · GetExchangeHandler)로 넘기고, 글 · 회원은 Fake Port로 만든다.
 */
class ExchangeLoginMemberControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    private static final Instant NOW = Instant.parse("2026-11-21T05:00:00Z");
    private static final String CREATE_URL = "/api/v1/exchanges";
    private static final String DETAIL_URL = "/api/v1/exchanges/{exchangeId}";

    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;
    /** 예전 컨트롤러가 하드코딩하던 회원 id */
    private static final Long LEGACY_HARDCODED_MEMBER = 200L;

    private static final Long POST_P = 10L;
    private static final Long POST_Q_OF_A = 21L;
    private static final Long POST_OF_C = 22L;
    private static final Long EXCHANGE_E = 1L;
    private static final Long ITEM_ID = 31L;

    private FakeExchangeRepository exchangeRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        commandBus = new FakeCommandBus(new UpdateNewItemHandler(exchangeRepository, new FakeAppointmentRepository(exchangeRepository, NOW),
                new FakePostLockRepository(), new FakeImagePort()),
                new CreateExchangeHandler(exchangeRepository, postPort, userPort, new FakeImagePort(), new FakeChatRoomPort()));
        queryBus = new FakeQueryBus(new GetExchangeHandler(exchangeRepository,
                new FakeAppointmentRepository(exchangeRepository, NOW), postPort, userPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))), new FakeChatRoomPort()));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ExchangeController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        // 공통 Given: 글 P는 교환 가능, 게시자 B
        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_Q_OF_A, "우사기 미니 피규어", null, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_OF_C, "하치와레 키링", null, MEMBER_C, PostInfo.Status.AVAILABLE));
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 2));
        userPort.register(new UserInfo(MEMBER_B, "하치", 5));
        userPort.register(new UserInfo(MEMBER_C, "쿠로미덕후", 1));
        userPort.register(new UserInfo(LEGACY_HARDCODED_MEMBER, "고정회원", 7));
    }

    // ---- E-1 ----

    @Test
    @DisplayName("E-1 A로 로그인해 요청하면 201이고 요청자는 A, 게시자는 B다")
    void createExchange_loginMember_becomesRequester() throws Exception {
        // when & then
        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requester.userId").value(MEMBER_A))
                .andExpect(jsonPath("$.data.owner.userId").value(MEMBER_B));

        // then: 버스로 간 커맨드 · 쿼리와 저장된 교환 모두 A 기준이다
        CreateExchangeCommand command = assertInstanceOf(CreateExchangeCommand.class,
                commandBus.sentCommands().get(0));
        assertEquals(MEMBER_A, command.requesterId());
        GetExchangeQuery query = assertInstanceOf(GetExchangeQuery.class, queryBus.sentQueries().get(0));
        assertEquals(MEMBER_A, query.viewerId());
        List<Exchange> saved = exchangeRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(MEMBER_A, saved.get(0).getRequestUserId());
        assertEquals(MEMBER_B, saved.get(0).getOwnerId());
    }

    @Test
    @DisplayName("E-1 회원 200이 아닌 회원이 요청해도 요청자는 로그인 회원이다")
    void createExchange_notLegacyMember_requesterIsLoginMember() throws Exception {
        // 게시자가 200인 글이라도 로그인 회원 C의 요청은 본인 글 요청이 아니다
        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, LEGACY_HARDCODED_MEMBER,
                PostInfo.Status.AVAILABLE));

        mockMvc.perform(createRequest(MEMBER_C, newItemBody(POST_P)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requester.userId").value(MEMBER_C))
                .andExpect(jsonPath("$.data.owner.userId").value(LEGACY_HARDCODED_MEMBER));
    }

    @Test
    @DisplayName("E-1 게시자 B로 로그인해 본인 글에 요청하면 400 EXCHANGE_003이고 교환이 저장되지 않는다")
    void createExchange_ownPost_returnsExchange003() throws Exception {
        mockMvc.perform(createRequest(MEMBER_B, newItemBody(POST_P)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_003"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 로그인 정보가 없으면 401 AUTH_001이고 교환이 저장되지 않는다")
    void createExchange_withoutLoginMember_returnsAuth001() throws Exception {
        mockMvc.perform(post(CREATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newItemBody(POST_P)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 A가 본인 글 Q를 POST 방식으로 첨부하면 201이고 요청자는 A다")
    void createExchange_attachOwnPost_returns201() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q_OF_A)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requester.userId").value(MEMBER_A))
                .andExpect(jsonPath("$.data.offerType").value("POST"));

        List<Exchange> saved = exchangeRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(MEMBER_A, saved.get(0).getRequestUserId());
        assertEquals(POST_Q_OF_A, saved.get(0).getOfferedItems().get(0).getSourcePostId());
    }

    @Test
    @DisplayName("E-1 A가 남의 글을 POST 방식으로 첨부하면 409 EXCHANGE_007이고 교환이 저장되지 않는다")
    void createExchange_attachOthersPost_returnsExchange007() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_OF_C)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_007"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 생성 응답의 요청자 · 게시자에 닉네임과 교환 완료 횟수가 채워진다")
    void createExchange_responseHasParticipantSummaries() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requester.userId").value(MEMBER_A))
                .andExpect(jsonPath("$.data.requester.nickname").value("바꾸"))
                .andExpect(jsonPath("$.data.requester.completedCount").value(2))
                .andExpect(jsonPath("$.data.owner.userId").value(MEMBER_B))
                .andExpect(jsonPath("$.data.owner.nickname").value("하치"))
                .andExpect(jsonPath("$.data.owner.completedCount").value(5));
    }

    // ---- E-1 차단 관계 (AC-9 · AC-10) ----

    @Test
    @DisplayName("E-1 A가 B를 차단했으면 NEW 방식 요청은 403 EXCHANGE_008이고 교환이 저장되지 않는다")
    void createExchange_requesterBlockedOwner_newItem_returnsExchange008() throws Exception {
        userPort.block(MEMBER_A, MEMBER_B);

        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_008"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 B가 A를 차단했으면 POST 방식 요청은 403 EXCHANGE_008이고 교환이 저장되지 않는다")
    void createExchange_ownerBlockedRequester_post_returnsExchange008() throws Exception {
        userPort.block(MEMBER_B, MEMBER_A);

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q_OF_A)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXCHANGE_008"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 차단을 해제하면 같은 요청이 201이다")
    void createExchange_afterUnblock_returns201() throws Exception {
        userPort.block(MEMBER_A, MEMBER_B);
        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P)))
                .andExpect(status().isForbidden());

        userPort.unblock(MEMBER_A, MEMBER_B);

        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requester.userId").value(MEMBER_A));
        assertEquals(1, exchangeRepository.findAll().size());
    }

    @Test
    @DisplayName("E-1 차단 관계이면서 남의 글을 첨부하면 403 EXCHANGE_008이다")
    void createExchange_blockedWithOthersAttachedPost_returnsExchange008() throws Exception {
        userPort.block(MEMBER_A, MEMBER_B);

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_OF_C)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXCHANGE_008"));
    }

    @Test
    @DisplayName("E-1 차단 관계이면서 글이 예약중이면 409 EXCHANGE_108이다")
    void createExchange_blockedWithReservedPost_returnsExchange108() throws Exception {
        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, MEMBER_B, PostInfo.Status.RESERVED));
        userPort.block(MEMBER_A, MEMBER_B);

        // #107 결정 5: 예약중 글은 EXCHANGE_108
        mockMvc.perform(createRequest(MEMBER_A, newItemBody(POST_P)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_108"));
    }

    @Test
    @DisplayName("E-1 차단 관계이면서 없는 글이면 404 EXCHANGE_009이다")
    void createExchange_blockedWithMissingPost_returnsExchange009() throws Exception {
        userPort.block(MEMBER_A, MEMBER_B);

        mockMvc.perform(createRequest(MEMBER_A, newItemBody(999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXCHANGE_009"));
    }

    // ---- E-2 ----

    @Test
    @DisplayName("E-2 요청자 A로 로그인해 조회하면 200")
    void getExchange_byRequester_returns200() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(EXCHANGE_E));

        GetExchangeQuery query = assertInstanceOf(GetExchangeQuery.class, queryBus.sentQueries().get(0));
        assertEquals(EXCHANGE_E, query.exchangeId());
        assertEquals(MEMBER_A, query.viewerId());
    }

    @Test
    @DisplayName("E-2 게시자 B로 로그인해 조회하면 200")
    void getExchange_byOwner_returns200() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));

        mockMvc.perform(detailRequest(MEMBER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(EXCHANGE_E));

        GetExchangeQuery query = assertInstanceOf(GetExchangeQuery.class, queryBus.sentQueries().get(0));
        assertEquals(MEMBER_B, query.viewerId());
    }

    @Test
    @DisplayName("E-2 참여자가 아닌 C로 로그인해 조회하면 403 EXCHANGE_002")
    void getExchange_byNonParticipant_returnsExchange002() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));

        mockMvc.perform(detailRequest(MEMBER_C))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_002"));
    }

    @Test
    @DisplayName("E-2 로그인 정보가 없으면 401 AUTH_001")
    void getExchange_withoutLoginMember_returnsAuth001() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));

        mockMvc.perform(get(DETAIL_URL, EXCHANGE_E))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        assertTrue(queryBus.sentQueries().isEmpty());
    }

    @Test
    @DisplayName("E-2 회원 200이 참여자가 아닌 교환을 200으로 로그인해 조회하면 403 EXCHANGE_002")
    void getExchange_byLegacyHardcodedMember_returnsExchange002() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));

        mockMvc.perform(detailRequest(LEGACY_HARDCODED_MEMBER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXCHANGE_002"));
    }

    @Test
    @DisplayName("E-2 응답의 요청자 · 게시자에 닉네임과 교환 완료 횟수가 채워진다")
    void getExchange_responseHasParticipantSummaries() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requester.userId").value(MEMBER_A))
                .andExpect(jsonPath("$.data.requester.nickname").value("바꾸"))
                .andExpect(jsonPath("$.data.requester.completedCount").value(2))
                .andExpect(jsonPath("$.data.owner.userId").value(MEMBER_B))
                .andExpect(jsonPath("$.data.owner.nickname").value("하치"))
                .andExpect(jsonPath("$.data.owner.completedCount").value(5));
    }

    @Test
    @DisplayName("E-2 게시자가 탈퇴했으면 200이고 게시자 닉네임은 탈퇴한 사용자, id와 완료 횟수는 유지한다")
    void getExchange_withdrawnOwner_showsWithdrawnNickname() throws Exception {
        exchangeRepository.save(exchangeE(MEMBER_A, MEMBER_B));
        userPort.register(new UserInfo(MEMBER_B, "탈퇴한 사용자", 5));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner.userId").value(MEMBER_B))
                .andExpect(jsonPath("$.data.owner.nickname").value("탈퇴한 사용자"))
                .andExpect(jsonPath("$.data.owner.completedCount").value(5));
    }

    @Test
    @DisplayName("E-2 게시자 회원 정보가 없으면 200이고 게시자 닉네임은 null, 완료 횟수는 0이다")
    void getExchange_missingOwnerMember_returnsNullNickname() throws Exception {
        Long missingOwner = 100L;
        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, missingOwner, PostInfo.Status.AVAILABLE));
        exchangeRepository.save(exchangeE(MEMBER_A, missingOwner));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner.userId").value(missingOwner))
                .andExpect(jsonPath("$.data.owner.nickname").doesNotExist())
                .andExpect(jsonPath("$.data.owner.completedCount").value(0))
                .andExpect(jsonPath("$.data.requester.nickname").value("바꾸"))
                .andExpect(jsonPath("$.data.requester.completedCount").value(2));
    }

    // ---- helpers ----

    private static MockHttpServletRequestBuilder createRequest(Long memberId, String body) {
        return post(CREATE_URL)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder detailRequest(Long memberId) {
        return get(DETAIL_URL, EXCHANGE_E)
                .requestAttr(LoginMember.ATTRIBUTE, memberId);
    }

    private static String newItemBody(Long postId) {
        return "{\"postId\": " + postId + ", \"offerType\": \"NEW\", \"newItem\": "
                + "{\"imageKeys\": [\"exchange/tmp/a.jpg\"], \"name\": \"치이카와 아크릴 스탠드\", "
                + "\"opened\": false, \"description\": \"가챠에서 두 개 나와서 하나 교환해요\"}}";
    }

    private static String postOfferBody(Long postId, Long offeredPostId) {
        return "{\"postId\": " + postId + ", \"offerType\": \"POST\", \"offeredPostIds\": [" + offeredPostId + "]}";
    }

    private static Exchange exchangeE(Long requesterId, Long ownerId) {
        return Exchange.of(EXCHANGE_E, POST_P, requesterId, ownerId, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }
}
