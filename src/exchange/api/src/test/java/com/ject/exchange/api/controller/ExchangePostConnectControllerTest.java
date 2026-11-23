package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.service.command.CreateExchangeHandler;
import com.ject.exchange.application.service.command.UpdateNewItemHandler;
import com.ject.exchange.application.service.query.GetExchangeHandler;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E-1 · E-2가 post BC 글 정보 · 글 상태로 판정하고, 사진 key를 URL로 바꿔 응답하는지 확인한다 (#107).
 * 버스는 실제 Handler로 넘기고, 글 · 회원 · 저장소는 Fake로 만든다.
 */
class ExchangePostConnectControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");
    private static final Instant NOW = Instant.parse("2026-11-21T05:00:00Z");
    private static final String CREATE_URL = "/api/v1/exchanges";
    private static final String DETAIL_URL = "/api/v1/exchanges/{exchangeId}";

    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    /** B의 글 P */
    private static final Long POST_P = 10L;
    /** A의 글 Q1 · Q2 */
    private static final Long POST_Q1 = 20L;
    private static final Long POST_Q2 = 21L;
    /** 다른 교환(A가 D의 글에 Q1을 첨부) */
    private static final Long POST_OF_D = 40L;
    private static final Long MEMBER_D = 104L;
    private static final Long MISSING_POST = 999L;
    private static final String KEY_P = "post/10/1.jpg";
    private static final String KEY_Q1 = "post/20/1.jpg";
    private static final String URL_P = "https://img.test/api/v1/images/post/10/1.jpg";
    private static final String URL_Q1 = "https://img.test/api/v1/images/post/20/1.jpg";

    private static final Long EXCHANGE_X = 1L;
    private static final Long OTHER_EXCHANGE = 2L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final Long OTHER_ITEM_Q1 = 51L;
    private static final Long APPOINTMENT_IN_X = 3L;
    private static final Long APPOINTMENT_IN_OTHER = 13L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postPort = new FakePostPort();
        FakeUserPort userPort = new FakeUserPort();
        FakeCommandBus commandBus = new FakeCommandBus(new UpdateNewItemHandler(exchangeRepository),
                new CreateExchangeHandler(exchangeRepository, postPort, userPort));
        FakeQueryBus queryBus = new FakeQueryBus(
                new GetExchangeHandler(exchangeRepository, appointmentRepository, postPort, userPort));
        mockMvc = MockMvcBuilders.standaloneSetup(new ExchangeController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        // 공통 Given: P(B, 대표 사진 key) · Q1(A, 대표 사진 key) · Q2(A, 사진 없음)
        postPort.register(postInfo(POST_P, "P", KEY_P, MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(postInfo(POST_Q2, "Q2", null, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(postInfo(POST_OF_D, "D의 글", null, MEMBER_D, PostInfo.Status.AVAILABLE));
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 2));
        userPort.register(new UserInfo(MEMBER_B, "하치", 5));
    }

    // ---- E-1 ----

    @Test
    @DisplayName("E-1 A가 P에 Q1을 첨부해 요청하면 201이고 게시자는 B · post는 P의 현재 정보다")
    void createExchange_postOffer_returnsPostFromPostBc() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.owner.userId").value(MEMBER_B))
                .andExpect(jsonPath("$.data.post.id").value(POST_P))
                .andExpect(jsonPath("$.data.post.title").value("P"))
                .andExpect(jsonPath("$.data.post.status").value("AVAILABLE"));

        List<Exchange> saved = exchangeRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(MEMBER_B, saved.get(0).getOwnerId());
    }

    @Test
    @DisplayName("E-1 생성 응답의 글 · 첨부 글 대표 사진은 key로 만든 URL이다")
    void createExchange_response_resolvesThumbnailUrls() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.post.thumbnailUrl").value(URL_P))
                .andExpect(jsonPath("$.data.offeredItems[0].post.thumbnailUrl").value(URL_Q1));
    }

    @Test
    @DisplayName("E-1 post BC에 없는 글이면 404 EXCHANGE_009이고 교환이 저장되지 않는다")
    void createExchange_missingPost_returnsExchange009() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(MISSING_POST, POST_Q1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_009"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 삭제된 글이면 404 EXCHANGE_009이고 교환이 저장되지 않는다")
    void createExchange_deletedPost_returnsExchange009() throws Exception {
        postPort.register(postInfo(POST_P, "P", KEY_P, MEMBER_B, PostInfo.Status.DELETED));

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EXCHANGE_009"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 예약중 글이면 409 EXCHANGE_108이고 교환이 저장되지 않는다")
    void createExchange_reservedPost_returnsExchange108() throws Exception {
        postPort.register(postInfo(POST_P, "P", KEY_P, MEMBER_B, PostInfo.Status.RESERVED));

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_108"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 교환 완료 글이면 409 EXCHANGE_005이고 교환이 저장되지 않는다")
    void createExchange_completedPost_returnsExchange005() throws Exception {
        postPort.register(postInfo(POST_P, "P", KEY_P, MEMBER_B, PostInfo.Status.COMPLETED));

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_005"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"DELETED", "RESERVED", "COMPLETED"})
    @DisplayName("E-1 첨부 글 Q1이 삭제 · 예약중 · 교환 완료면 409 EXCHANGE_007이다")
    void createExchange_unavailableAttachedPost_returnsExchange007(PostInfo.Status status) throws Exception {
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, status));

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_007"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-1 첨부 글이 post BC에 없으면 409 EXCHANGE_007이다")
    void createExchange_missingAttachedPost_returnsExchange007() throws Exception {
        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, MISSING_POST)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCHANGE_007"));

        assertTrue(exchangeRepository.isEmpty());
    }

    // ---- E-2 대표 사진 (AC-6) ----

    @Test
    @DisplayName("E-2 글 · 첨부 글 대표 사진은 key로 만든 URL이고 key가 없으면 null이다")
    void getExchange_resolvesThumbnailUrls() throws Exception {
        exchangeRepository.save(exchangeX());

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.id").value(POST_P))
                .andExpect(jsonPath("$.data.post.title").value("P"))
                .andExpect(jsonPath("$.data.post.thumbnailUrl").value(URL_P))
                .andExpect(jsonPath("$.data.post.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.offeredItems[0].id").value(ITEM_Q1))
                .andExpect(jsonPath("$.data.offeredItems[0].post.thumbnailUrl").value(URL_Q1))
                .andExpect(jsonPath("$.data.offeredItems[1].id").value(ITEM_Q2))
                .andExpect(jsonPath("$.data.offeredItems[1].post.title").value("Q2"))
                .andExpect(jsonPath("$.data.offeredItems[1].post.thumbnailUrl").doesNotExist());
    }

    // ---- E-2 삭제된 글 (AC-7) ----

    @Test
    @DisplayName("E-2 교환글 P가 삭제됐으면 post는 id만 두고 상태 DELETED · 제목 · 사진 null이다")
    void getExchange_deletedPost_showsDeleted() throws Exception {
        postPort.register(postInfo(POST_P, "P", KEY_P, MEMBER_B, PostInfo.Status.DELETED));
        exchangeRepository.save(exchangeX());

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.id").value(POST_P))
                .andExpect(jsonPath("$.data.post.status").value("DELETED"))
                .andExpect(jsonPath("$.data.post.title").doesNotExist())
                .andExpect(jsonPath("$.data.post.thumbnailUrl").doesNotExist());
    }

    @Test
    @DisplayName("E-2 교환글 P가 post BC에 없으면 post 상태 DELETED · 제목 · 사진 null이다")
    void getExchange_missingPost_showsDeleted() throws Exception {
        exchangeRepository.save(Exchange.of(EXCHANGE_X, MISSING_POST, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.id").value(MISSING_POST))
                .andExpect(jsonPath("$.data.post.status").value("DELETED"))
                .andExpect(jsonPath("$.data.post.title").doesNotExist())
                .andExpect(jsonPath("$.data.post.thumbnailUrl").doesNotExist());
    }

    @Test
    @DisplayName("E-2 첨부 글 Q1이 삭제됐으면 availability DELETED · post 상태 DELETED · 제목 · 사진 null이다")
    void getExchange_deletedAttachedPost_showsDeleted() throws Exception {
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.DELETED));
        exchangeRepository.save(exchangeX());

        mockMvc.perform(detailRequest(MEMBER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("DELETED"))
                .andExpect(jsonPath("$.data.offeredItems[0].post.id").value(POST_Q1))
                .andExpect(jsonPath("$.data.offeredItems[0].post.status").value("DELETED"))
                .andExpect(jsonPath("$.data.offeredItems[0].post.title").doesNotExist())
                .andExpect(jsonPath("$.data.offeredItems[0].post.thumbnailUrl").doesNotExist())
                .andExpect(jsonPath("$.data.offeredItems[1].availability").value("AVAILABLE"));
    }

    @Test
    @DisplayName("E-2 첨부 글 Q1이 post BC에 없으면 availability DELETED · 제목 null이다")
    void getExchange_missingAttachedPost_showsDeleted() throws Exception {
        exchangeRepository.save(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, MISSING_POST, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("DELETED"))
                .andExpect(jsonPath("$.data.offeredItems[0].post.status").value("DELETED"))
                .andExpect(jsonPath("$.data.offeredItems[0].post.title").doesNotExist())
                .andExpect(jsonPath("$.data.offeredItems[0].post.thumbnailUrl").doesNotExist());
    }

    @Test
    @DisplayName("E-2 첨부 글 Q1이 교환 완료면 availability COMPLETED다")
    void getExchange_completedAttachedPost_showsCompleted() throws Exception {
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.COMPLETED));
        exchangeRepository.save(exchangeX());

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("COMPLETED"))
                .andExpect(jsonPath("$.data.offeredItems[0].post.title").value("Q1"));
    }

    // ---- E-2 이 교환에 묶인 첨부 글 (AC-8) ----

    @Test
    @DisplayName("E-2 X의 확정 약속이 Q1을 골라 Q1이 예약중이면 Q1 RESERVED_HERE · Q2 AVAILABLE이다")
    void getExchange_attachedPostReservedHere_showsReservedHere() throws Exception {
        // given
        exchangeRepository.save(exchangeX());
        appointmentRepository.preload(confirmed(APPOINTMENT_IN_X, EXCHANGE_X, MEMBER_A, MEMBER_B, List.of(ITEM_Q1)));
        postPort.register(postInfo(POST_P, "P", KEY_P, MEMBER_B, PostInfo.Status.RESERVED));
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.RESERVED));

        // when & then
        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.status").value("RESERVED"))
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("RESERVED_HERE"))
                .andExpect(jsonPath("$.data.offeredItems[0].post.status").value("RESERVED"))
                .andExpect(jsonPath("$.data.offeredItems[1].availability").value("AVAILABLE"));
    }

    @Test
    @DisplayName("E-2 Q1이 다른 교환의 확정 약속으로 예약중이면 Q1 RESERVED_ELSEWHERE다")
    void getExchange_attachedPostReservedElsewhere_showsReservedElsewhere() throws Exception {
        // given: A가 D의 글에 Q1을 첨부한 다른 교환에서 Q1을 고른 약속이 확정됨
        exchangeRepository.save(exchangeX());
        exchangeRepository.save(Exchange.of(OTHER_EXCHANGE, POST_OF_D, MEMBER_A, MEMBER_D, OfferType.POST,
                List.of(OfferedItem.of(OTHER_ITEM_Q1, POST_Q1, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
        appointmentRepository.preload(confirmed(APPOINTMENT_IN_OTHER, OTHER_EXCHANGE, MEMBER_A, MEMBER_D,
                List.of(OTHER_ITEM_Q1)));
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.RESERVED));

        // when & then
        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("RESERVED_ELSEWHERE"))
                .andExpect(jsonPath("$.data.offeredItems[1].availability").value("AVAILABLE"));
    }

    @Test
    @DisplayName("E-2 X의 확정 약속이 Q2를 골랐으면 예약중인 Q1은 RESERVED_ELSEWHERE · Q2는 RESERVED_HERE다")
    void getExchange_confirmedChoseOtherItem_showsElsewhereForUnchosen() throws Exception {
        exchangeRepository.save(exchangeX());
        appointmentRepository.preload(confirmed(APPOINTMENT_IN_X, EXCHANGE_X, MEMBER_A, MEMBER_B, List.of(ITEM_Q2)));
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.RESERVED));
        postPort.register(postInfo(POST_Q2, "Q2", null, MEMBER_A, PostInfo.Status.RESERVED));

        mockMvc.perform(detailRequest(MEMBER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("RESERVED_ELSEWHERE"))
                .andExpect(jsonPath("$.data.offeredItems[1].availability").value("RESERVED_HERE"));
    }

    @Test
    @DisplayName("E-2 X의 약속이 제안 상태뿐이면 예약중인 Q1은 RESERVED_ELSEWHERE다")
    void getExchange_onlyProposedInExchange_showsElsewhere() throws Exception {
        exchangeRepository.save(exchangeX());
        appointmentRepository.preload(Appointment.of(APPOINTMENT_IN_X, EXCHANGE_X, MEMBER_A, MEMBER_B,
                AppointmentStatus.PROPOSED, scheduled(), PLACE, new AppointmentItemIds(List.of(ITEM_Q1)),
                NOW.plus(Duration.ofHours(23)), 30, null, null, NOW, NOW));
        postPort.register(postInfo(POST_Q1, "Q1", KEY_Q1, MEMBER_A, PostInfo.Status.RESERVED));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeredItems[0].availability").value("RESERVED_ELSEWHERE"));
    }

    // ---- 알 수 없는 글 상태 (AC-17) ----

    @Test
    @DisplayName("E-1 글 상태를 알 수 없어 조회가 실패하면 500 INTERNAL_001이고 교환이 저장되지 않는다")
    void createExchange_unknownPostStatus_returnsInternal001() throws Exception {
        postPort.failLookupWith(new IllegalStateException("알 수 없는 글 상태: HIDDEN"));

        mockMvc.perform(createRequest(MEMBER_A, postOfferBody(POST_P, POST_Q1)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_001"));

        assertTrue(exchangeRepository.isEmpty());
    }

    @Test
    @DisplayName("E-2 글 상태를 알 수 없어 조회가 실패하면 500 INTERNAL_001이다")
    void getExchange_unknownPostStatus_returnsInternal001() throws Exception {
        exchangeRepository.save(exchangeX());
        postPort.failLookupWith(new IllegalStateException("알 수 없는 글 상태: HIDDEN"));

        mockMvc.perform(detailRequest(MEMBER_A))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_001"));
    }

    // ---- helpers ----

    private static MockHttpServletRequestBuilder createRequest(Long memberId, String body) {
        return post(CREATE_URL)
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder detailRequest(Long memberId) {
        return get(DETAIL_URL, EXCHANGE_X).requestAttr(LoginMember.ATTRIBUTE, memberId);
    }

    private static String postOfferBody(Long postId, Long offeredPostId) {
        return "{\"postId\": " + postId + ", \"offerType\": \"POST\", \"offeredPostIds\": [" + offeredPostId + "]}";
    }

    /** 교환 X: A가 P에 Q1 · Q2를 첨부 */
    private static Exchange exchangeX() {
        return Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0),
                        OfferedItem.of(ITEM_Q2, POST_Q2, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }

    private static Appointment confirmed(Long id, Long exchangeId, Long proposerId, Long receiverId,
                                         List<Long> itemIds) {
        return Appointment.of(id, exchangeId, proposerId, receiverId, AppointmentStatus.CONFIRMED, scheduled(),
                PLACE, new AppointmentItemIds(itemIds), NOW.minus(Duration.ofHours(1)), 30, null,
                NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(3)), NOW.minus(Duration.ofHours(3)));
    }

    private static AppointmentTime scheduled() {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, NOW.plus(Duration.ofDays(1)));
    }

    private static PostInfo postInfo(Long id, String title, String thumbnailKey, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, title, thumbnailKey, ownerId, status);
    }
}
