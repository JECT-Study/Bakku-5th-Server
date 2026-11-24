package com.ject.post.api.controller;

import com.ject.common.core.cqrs.command.Command;
import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.post.api.exception.PostExceptionHandler;
import com.ject.post.application.dto.AuthorInfo;
import com.ject.post.application.dto.PostResult;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.application.dto.command.DeletePostCommand;
import com.ject.post.application.dto.command.IncreaseViewCountCommand;
import com.ject.post.application.dto.command.UpdatePostCommand;
import com.ject.post.application.dto.query.GetPostQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerTest {

    private static final String CREATE_BODY = """
            {
              "imageKeys": ["post/tmp/3/8f14e45f.jpg", "post/tmp/3/c9f0f895.jpg"],
              "title": "치이카와 하치와레 가챠 키링",
              "category": "CHARACTER",
              "opened": false,
              "tags": ["치이카와", "하치와레"],
              "description": "치이카와 가챠 3탄에서 뽑았어요.",
              "wantedItem": "치이카와 가챠 키링",
              "place": {
                "lat": 37.5563,
                "lng": 126.9236,
                "name": "스타벅스 홍대동교점",
                "regionName": "서울특별시 마포구 동교동",
                "memo": "AK플라자 주변"
              },
              "nowExchangeMinutes": 60
            }
            """;

    private static final String UPDATE_BODY = """
            {
              "imageKeys": ["post/10/8f14e45f.jpg", "post/tmp/3/c9f0f895.jpg"],
              "title": "치이카와 하치와레 가챠 키링",
              "category": "CHARACTER",
              "opened": true,
              "tags": ["치이카와"],
              "description": "한 번 개봉했어요.",
              "wantedItem": null,
              "place": {
                "lat": 37.5563,
                "lng": 126.9236,
                "name": "스타벅스 홍대동교점",
                "regionName": "서울특별시 마포구 동교동",
                "memo": null
              }
            }
            """;

    private final List<Object> sent = new ArrayList<>();
    private final FakeCommandBus commandBus = new FakeCommandBus();
    private final FakeQueryBus queryBus = new FakeQueryBus();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostController controller = new PostController(commandBus, queryBus,
                new ImageUrlResolver("http://localhost:8080"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PostExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("PO-1 작성하면 201과 조회 결과를 반환하고, 사진 URL은 IMG-2 주소다")
    void createPost_returns201WithPost() throws Exception {
        createPost(CREATE_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.images[0].key").value("post/10/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.images[0].url")
                        .value("http://localhost:8080/api/v1/images/post/10/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.place.regionName").value("서울특별시 마포구 동교동"))
                .andExpect(jsonPath("$.data.isMine").value(true))
                .andExpect(jsonPath("$.data.nowExchangeEndsAt").value("2026-11-02T15:30:00+09:00"))
                .andExpect(jsonPath("$.data.author.completedCount").value(12))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("chatRoomId"), nullValue())))
                .andExpect(jsonPath("$.data.canRequest").doesNotExist());
    }

    @Test
    @DisplayName("PO-1 작성자는 토큰의 회원이고, 요청 값을 그대로 Command로 넘긴다")
    void createPost_sendsCommandWithLoginMember() throws Exception {
        createPost(CREATE_BODY);

        CreatePostCommand command = (CreatePostCommand) commandBus.received;
        assertEquals(3L, command.authorId());
        assertEquals("CHARACTER", command.category());
        assertEquals("서울특별시 마포구 동교동", command.place().regionName());
        assertEquals(List.of("치이카와", "하치와레"), command.tags());
        assertEquals(60, command.nowExchangeMinutes());
    }

    @Test
    @DisplayName("PO-1 wantedItem을 생략하면 null(아무거나)로 넘긴다")
    void createPost_withoutWantedItem_sendsNull() throws Exception {
        createPost(CREATE_BODY.replace("\"wantedItem\": \"치이카와 가챠 키링\",", ""));

        assertNull(((CreatePostCommand) commandBus.received).wantedItem());
    }

    @Test
    @DisplayName("PO-1 로그인 정보가 없으면 AUTH_001")
    void createPost_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("PO-1 Idempotency-Key 헤더가 없으면 COMMON_001")
    void createPost_withoutIdempotencyKey_returnsCommon001() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .requestAttr(LoginMember.ATTRIBUTE, 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
    }

    @Test
    @DisplayName("PO-1 wantedItem이 공백뿐이면 COMMON_001")
    void createPost_blankWantedItem_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("\"치이카와 가챠 키링\"", "\"  \"")));
    }

    @Test
    @DisplayName("PO-1 imageKeys가 중복되면 COMMON_001")
    void createPost_duplicateImageKeys_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace(
                "[\"post/tmp/3/8f14e45f.jpg\", \"post/tmp/3/c9f0f895.jpg\"]",
                "[\"post/tmp/3/8f14e45f.jpg\", \"post/tmp/3/8f14e45f.jpg\"]")));
    }

    @Test
    @DisplayName("PO-1 태그에 #이 있으면 COMMON_001")
    void createPost_tagWithHash_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("[\"치이카와\", \"하치와레\"]", "[\"#치이카와\"]")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"용산", "마포구 동교동", "서울 마포구 동교동", "서울특별시", "서울특별시 a b c d"})
    @DisplayName("PO-1 지역이 시/도 + 1~3단어 형식이 아니면 COMMON_001")
    void createPost_invalidRegionName_returnsCommon001(String regionName) throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("서울특별시 마포구 동교동", regionName)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"경기도 성남시 분당구 정자동", "세종특별자치시 조치원읍", "강원도 춘천시 효자동",
            "강원특별자치도 춘천시 효자동", "전북특별자치도 전주시 완산구 중앙동"})
    @DisplayName("PO-1 지역이 시/도로 시작하는 2~4단어면 받는다")
    void createPost_validRegionName_returns201(String regionName) throws Exception {
        createPost(CREATE_BODY.replace("서울특별시 마포구 동교동", regionName))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PO-1 지금 교환 시간이 15·30·60·120·180분이 아니면 COMMON_001")
    void createPost_nowExchange45_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("\"nowExchangeMinutes\": 60", "\"nowExchangeMinutes\": 45")));
    }

    @Test
    @DisplayName("PO-3 상세는 조회 결과에 canRequest를 포함하고, 사진 URL은 IMG-2 주소다")
    void getPost_includesCanRequest() throws Exception {
        queryBus.mine = false;

        getPost("10", 99L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.isMine").value(false))
                .andExpect(jsonPath("$.data.canRequest").value(true))
                .andExpect(jsonPath("$.data.images[0].url")
                        .value("http://localhost:8080/api/v1/images/post/10/8f14e45f.jpg"));
    }

    @Test
    @DisplayName("PO-3 조회 수를 먼저 올리고 조회한다. 보는 사람은 토큰의 회원이다")
    void getPost_increasesViewCountBeforeQuery() throws Exception {
        getPost("10", 99L);

        assertEquals(List.of(new IncreaseViewCountCommand(10L, 99L), new GetPostQuery(10L, 99L)), sent);
    }

    @Test
    @DisplayName("PO-3 내 글이면 canRequest는 false다")
    void getPost_myPost_cannotRequest() throws Exception {
        getPost("10", 3L)
                .andExpect(jsonPath("$.data.isMine").value(true))
                .andExpect(jsonPath("$.data.canRequest").value(false));
    }

    @Test
    @DisplayName("PO-3 로그인 정보가 없으면 AUTH_001")
    void getPost_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(get("/api/v1/posts/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("PO-3 postId 형식이 틀리면 COMMON_001")
    void getPost_invalidId_returnsCommon001() throws Exception {
        expectCommon001(getPost("abc", 3L));
    }

    @Test
    @DisplayName("PO-4 수정하면 다시 조회한 결과를 반환하고, canRequest는 없다")
    void updatePost_returnsQueriedPost() throws Exception {
        updatePost(UPDATE_BODY, 3L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.images[0].url")
                        .value("http://localhost:8080/api/v1/images/post/10/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.canRequest").doesNotExist());
    }

    @Test
    @DisplayName("PO-4 수정자는 토큰의 회원이고, 요청 값을 Command로 넘긴 뒤 조회한다")
    void updatePost_sendsCommandThenQuery() throws Exception {
        updatePost(UPDATE_BODY, 3L);

        UpdatePostCommand command = (UpdatePostCommand) sent.get(0);
        assertEquals(10L, command.postId());
        assertEquals(3L, command.editorId());
        assertEquals(List.of("post/10/8f14e45f.jpg", "post/tmp/3/c9f0f895.jpg"), command.imageKeys());
        assertEquals("CHARACTER", command.category());
        assertNull(command.wantedItem());
        assertNull(command.place().memo());
        assertEquals(new GetPostQuery(10L, 3L), sent.get(1));
    }

    @Test
    @DisplayName("PO-4 로그인 정보가 없으면 AUTH_001")
    void updatePost_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(put("/api/v1/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("PO-4 제품명이 비어 있으면 COMMON_001")
    void updatePost_blankTitle_returnsCommon001() throws Exception {
        expectCommon001(updatePost(UPDATE_BODY.replace("\"치이카와 하치와레 가챠 키링\"", "\"  \""), 3L));
    }

    @Test
    @DisplayName("PO-4 지역이 시/도로 시작하지 않으면 COMMON_001")
    void updatePost_invalidRegionName_returnsCommon001() throws Exception {
        expectCommon001(updatePost(UPDATE_BODY.replace("서울특별시 마포구 동교동", "마포구 동교동"), 3L));
    }

    @Test
    @DisplayName("PO-5 삭제하면 data는 null이다")
    void deletePost_returnsNullData() throws Exception {
        deletePost("10", 3L)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":null}", JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("PO-5 삭제자는 토큰의 회원이고, Command만 보낸다")
    void deletePost_sendsCommandWithLoginMember() throws Exception {
        deletePost("10", 3L);

        assertEquals(List.of(new DeletePostCommand(10L, 3L)), sent);
    }

    @Test
    @DisplayName("PO-5 로그인 정보가 없으면 AUTH_001")
    void deletePost_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("PO-5 postId 형식이 틀리면 COMMON_001")
    void deletePost_invalidId_returnsCommon001() throws Exception {
        expectCommon001(deletePost("abc", 3L));
    }

    @Test
    @DisplayName("PO-6 지금 교환을 끄면 data는 null이다")
    void turnOffNowExchange_returnsNullData() throws Exception {
        mockMvc.perform(post("/api/v1/posts/10/now-exchange/off"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":null}", JsonCompareMode.STRICT));
    }

    private ResultActions createPost(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/posts")
                .requestAttr(LoginMember.ATTRIBUTE, 3L)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions updatePost(String body, Long memberId) throws Exception {
        return mockMvc.perform(put("/api/v1/posts/10")
                .requestAttr(LoginMember.ATTRIBUTE, memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions deletePost(String postId, Long memberId) throws Exception {
        return mockMvc.perform(delete("/api/v1/posts/" + postId)
                .requestAttr(LoginMember.ATTRIBUTE, memberId));
    }

    private ResultActions getPost(String postId, Long memberId) throws Exception {
        return mockMvc.perform(get("/api/v1/posts/" + postId)
                .requestAttr(LoginMember.ATTRIBUTE, memberId));
    }

    private static void expectCommon001(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    class FakeCommandBus implements CommandBus {

        Command<?> received;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Command<R> command) {
            received = command;
            sent.add(command);
            return command instanceof CreatePostCommand ? (R) Long.valueOf(10L) : null;
        }
    }

    class FakeQueryBus implements QueryBus {

        boolean mine = true;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Query<R> query) {
            sent.add(query);
            return (R) new PostResult(10L, "AVAILABLE",
                    List.of("post/10/8f14e45f.jpg", "post/10/c9f0f895.jpg"),
                    "치이카와 하치와레 가챠 키링", "CHARACTER", false, List.of("치이카와", "하치와레"),
                    "치이카와 가챠 3탄에서 뽑았어요.", "치이카와 가챠 키링",
                    new PostResult.PlaceResult(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", "AK플라자 주변"),
                    true, OffsetDateTime.parse("2026-11-02T15:30:00+09:00"), 0, 0,
                    new AuthorInfo(3L, "하치와레러버", 12), mine, null, !mine,
                    OffsetDateTime.parse("2026-11-02T14:30:00+09:00"));
        }
    }
}
