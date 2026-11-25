package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.post.api.exception.PostExceptionHandler;
import com.ject.post.application.dto.PostPageResult;
import com.ject.post.application.dto.PostSearchPageResult;
import com.ject.post.application.dto.PostSummaryResult;
import com.ject.post.application.dto.query.GetPostsQuery;
import com.ject.post.application.dto.query.SearchPostsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostDiscoveryControllerTest {

    private static final Long MEMBER_ID = 3L;
    private static final String CURSOR_ID_10 = "eyJpZCI6MTB9";
    private static final String SEARCH_CURSOR_RANK2_ID57 = "eyJyYW5rIjoyLCJpZCI6NTd9";

    private final FakeQueryBus queryBus = new FakeQueryBus();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostDiscoveryController controller = new PostDiscoveryController(queryBus,
                new ImageUrlResolver("http://localhost:8080"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PostExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("PO-2 홈 목록은 카드와 다음 커서를 돌려주고, 사진 URL은 IMG-2 주소다")
    void getPosts_returnsCursorPage() throws Exception {
        queryBus.result = new PostPageResult(List.of(summary(11L), summary(10L)), 10L, true);

        getPosts()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(11))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl")
                        .value("http://localhost:8080/api/v1/images/post/11/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.content[0].regionName").value("서울특별시 마포구 동교동"))
                .andExpect(jsonPath("$.data.content[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.content[0].nowExchangeEndsAt").value("2026-11-02T15:30:00+09:00"))
                .andExpect(jsonPath("$.data.content[0]", hasEntry(equalTo("wantedItem"), nullValue())))
                .andExpect(jsonPath("$.data.nextCursor").value(CURSOR_ID_10))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("PO-2 마지막 페이지면 nextCursor는 null이다")
    void getPosts_lastPage_nullCursor() throws Exception {
        queryBus.result = new PostPageResult(List.of(summary(1L)), null, false);

        getPosts()
                .andExpect(jsonPath("$.data", hasEntry(equalTo("nextCursor"), nullValue())))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("PO-2 필터 · 커서 · 보는 사람을 Query로 넘기고, size 기본값은 20이다")
    void getPosts_sendsQuery() throws Exception {
        getPosts().andExpect(status().isOk());
        assertEquals(new GetPostsQuery(MEMBER_ID, null, null, null, null, 20), queryBus.received);

        getPosts(get("/api/v1/posts")
                .param("nowExchange", "true")
                .param("category", "IDOL")
                .param("hub", "HONGDAE")
                .param("cursor", CURSOR_ID_10)
                .param("size", "5"));
        assertEquals(new GetPostsQuery(MEMBER_ID, true, "IDOL", "HONGDAE", 10L, 5), queryBus.received);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNCLASSIFIED", "GANGNAM"})
    @DisplayName("PO-2 미분류나 없는 거점이면 COMMON_001")
    void getPosts_invalidHub_returnsCommon001(String hub) throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts").param("hub", hub)));
    }

    @Test
    @DisplayName("PO-2 없어진 정렬 값(NEAREST)이면 COMMON_001")
    void getPosts_removedSort_returnsCommon001() throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts").param("sort", "NEAREST")));
    }

    @Test
    @DisplayName("PO-2 cursor 형식이 틀리면 COMMON_001")
    void getPosts_invalidCursor_returnsCommon001() throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts").param("cursor", "not-a-cursor")));
        expectCommon001(getPosts(get("/api/v1/posts").param("cursor", "eyJmb28iOjF9")));
        assertNull(queryBus.received);
    }

    @Test
    @DisplayName("PO-2 로그인 정보가 없으면 AUTH_001")
    void getPosts_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("PO-2 size가 1~50을 벗어나면 COMMON_001")
    void getPosts_sizeOutOfRange_returnsCommon001() throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts").param("size", "51")));
    }

    @Test
    @DisplayName("PO-2 없는 카테고리면 COMMON_001")
    void getPosts_invalidCategory_returnsCommon001() throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts").param("category", "FOOD")));
    }

    @Test
    @DisplayName("PO-7 검색어 · 카테고리 · 거점 · 교환 방식 · 커서(순위, id)를 Query로 넘기고, 다음 커서를 순위 · id로 만든다")
    void searchPosts_sendsQueryAndCursor() throws Exception {
        queryBus.searchResult = new PostSearchPageResult(List.of(summary(57L)), 3, 28L, true);

        getPosts(get("/api/v1/posts/search")
                .param("keyword", "# 치이 카와")
                .param("category", "IDOL")
                .param("hub", "SINDORIM")
                .param("nowExchange", "true")
                .param("cursor", SEARCH_CURSOR_RANK2_ID57)
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(57))
                .andExpect(jsonPath("$.data.nextCursor").value("eyJyYW5rIjozLCJpZCI6Mjh9"))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        assertEquals(new SearchPostsQuery(MEMBER_ID, "# 치이 카와", "IDOL", "SINDORIM", true, 2, 57L, 5),
                queryBus.received);
    }

    @Test
    @DisplayName("PO-7 1글자 검색어를 받고, 거점 · 교환 방식을 생략하면 null로 넘긴다")
    void searchPosts_oneCharacter_withoutFilters() throws Exception {
        getPosts(get("/api/v1/posts/search").param("keyword", "치"))
                .andExpect(status().isOk());

        assertEquals(new SearchPostsQuery(MEMBER_ID, "치", null, null, null, null, null, 20), queryBus.received);
    }

    @Test
    @DisplayName("PO-7 마지막 페이지면 nextCursor는 null이다")
    void searchPosts_lastPage_nullCursor() throws Exception {
        queryBus.searchResult = new PostSearchPageResult(List.of(summary(1L)), null, null, false);

        getPosts(get("/api/v1/posts/search").param("keyword", "치이카와"))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("nextCursor"), nullValue())));
    }

    @Test
    @DisplayName("PO-7 검색 커서 형식이 아니면(홈 목록 커서 포함) COMMON_001")
    void searchPosts_invalidCursor_returnsCommon001() throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts/search").param("keyword", "치이카와").param("cursor", CURSOR_ID_10)));
        expectCommon001(getPosts(get("/api/v1/posts/search").param("keyword", "치이카와").param("cursor", "not-a-cursor")));
    }

    @Test
    @DisplayName("PO-7 검색어가 없으면 COMMON_001")
    void searchPosts_withoutKeyword_returnsCommon001() throws Exception {
        expectCommon001(getPosts(get("/api/v1/posts/search")));
    }

    @Test
    @DisplayName("PO-7 로그인 정보가 없으면 AUTH_001")
    void searchPosts_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(get("/api/v1/posts/search").param("keyword", "치이카와"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    private ResultActions getPosts() throws Exception {
        return getPosts(get("/api/v1/posts"));
    }

    private ResultActions getPosts(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID));
    }

    private static PostSummaryResult summary(Long id) {
        return new PostSummaryResult(id, "post/" + id + "/8f14e45f.jpg", "치이카와 하치와레 가챠 키링",
                "서울특별시 마포구 동교동", "AVAILABLE", true, OffsetDateTime.parse("2026-11-02T15:30:00+09:00"),
                null, false, OffsetDateTime.parse("2026-11-02T14:30:00+09:00"));
    }

    private static void expectCommon001(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    static class FakeQueryBus implements QueryBus {

        PostPageResult result = new PostPageResult(List.of(), null, false);
        PostSearchPageResult searchResult = new PostSearchPageResult(List.of(), null, null, false);
        Query<?> received;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Query<R> query) {
            received = query;
            return (R) (query instanceof SearchPostsQuery ? searchResult : result);
        }
    }
}
