package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlockControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BlockController())
                .setControllerAdvice(new UserExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("BL-1 차단하면 201과 차단한 회원 · 시각을 반환한다")
    void block_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 202}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(202))
                .andExpect(jsonPath("$.data.blockedAt").value("2026-11-27T14:30:00+09:00"));
    }

    @Test
    @DisplayName("BL-1 userId가 없으면 COMMON_001")
    void block_missingUserId_returnsCommon001() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("BL-2 차단 목록을 커서 형식으로 반환한다")
    void getBlocks() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].nickname").value("하치와레러버"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("BL-2 size가 50을 넘으면 COMMON_001")
    void getBlocks_sizeOver50_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("BL-3 차단 해제하면 성공만 반환한다")
    void unblock() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/blocks/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
