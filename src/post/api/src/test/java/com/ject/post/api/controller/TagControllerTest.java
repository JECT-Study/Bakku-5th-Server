package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.post.api.exception.PostExceptionHandler;
import com.ject.post.application.dto.TagSuggestionResult;
import com.ject.post.application.dto.query.SuggestTagsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TagControllerTest {

    private final FakeQueryBus queryBus = new FakeQueryBus();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TagController(queryBus))
                .setControllerAdvice(new PostExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("PO-9 입력 글자와 size로 조회해 태그 이름 목록을 준다")
    void suggestTags_returnsTags() throws Exception {
        queryBus.result = new TagSuggestionResult(List.of("치이카와", "치이카와가챠"));

        mockMvc.perform(get("/api/v1/tags").param("keyword", "#치이").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tags[0]").value("치이카와"))
                .andExpect(jsonPath("$.data.tags[1]").value("치이카와가챠"));

        assertEquals(new SuggestTagsQuery("#치이", 5), queryBus.received);
    }

    @Test
    @DisplayName("size를 생략하면 10개")
    void suggestTags_defaultSize() throws Exception {
        mockMvc.perform(get("/api/v1/tags").param("keyword", "치이"))
                .andExpect(status().isOk());

        assertEquals(new SuggestTagsQuery("치이", 10), queryBus.received);
    }

    @Test
    @DisplayName("맞는 태그가 없으면 빈 배열")
    void suggestTags_empty() throws Exception {
        mockMvc.perform(get("/api/v1/tags").param("keyword", "모몽가"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags").isEmpty());
    }

    @Test
    @DisplayName("검색어가 없거나 size가 1~20이 아니면 COMMON_001")
    void suggestTags_invalidParams() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
        mockMvc.perform(get("/api/v1/tags").param("keyword", "치이").param("size", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    static class FakeQueryBus implements QueryBus {

        TagSuggestionResult result = new TagSuggestionResult(List.of());
        Query<?> received;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Query<R> query) {
            received = query;
            return (R) result;
        }
    }
}
