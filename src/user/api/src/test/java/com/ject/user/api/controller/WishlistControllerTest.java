package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WishlistController())
                .setControllerAdvice(new UserExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("WL-1 키워드 목록과 최대 개수를 반환한다")
    void getKeywords() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/wishlist-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keywords[0].keyword").value("치이카와"))
                .andExpect(jsonPath("$.data.maxCount").value(10));
    }

    @Test
    @DisplayName("WL-2 키워드를 추가하면 201과 저장한 키워드를 반환한다")
    void addKeyword_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/wishlist-keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": \"우사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.keyword").value("우사기"));
    }

    @Test
    @DisplayName("WL-2 키워드가 2자 미만이면 COMMON_001")
    void addKeyword_tooShort_returnsCommon001() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/wishlist-keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": \"치\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("WL-3 키워드를 삭제하면 성공만 반환한다")
    void deleteKeyword() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/wishlist-keywords/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
