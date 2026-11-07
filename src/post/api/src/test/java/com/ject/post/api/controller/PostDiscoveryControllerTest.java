package com.ject.post.api.controller;

import com.ject.post.api.exception.PostExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostDiscoveryControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PostDiscoveryController())
                .setControllerAdvice(new PostExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("PO-2 홈 목록은 커서 페이지로 응답한다")
    void getPosts_returnsCursorPage() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].regionName").value("마포구 동교동"))
                .andExpect(jsonPath("$.data.content[0].wantedAny").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").value("eyJpZCI6MTB9"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("PO-2 size가 1~50을 벗어나면 COMMON_001")
    void getPosts_sizeOutOfRange_returnsCommon001() throws Exception {
        expectCommon001(mockMvc.perform(get("/api/v1/posts").param("size", "51")));
    }

    @Test
    @DisplayName("PO-2 없는 카테고리면 COMMON_001")
    void getPosts_invalidCategory_returnsCommon001() throws Exception {
        expectCommon001(mockMvc.perform(get("/api/v1/posts").param("category", "FOOD")));
    }

    @Test
    @DisplayName("PO-7 검색어가 없으면 COMMON_001")
    void searchPosts_withoutKeyword_returnsCommon001() throws Exception {
        expectCommon001(mockMvc.perform(get("/api/v1/posts/search")));
    }

    @Test
    @DisplayName("PO-8 지도 숫자 핀 목록을 반환한다")
    void getMapMarkers_returnsMarkers() throws Exception {
        mockMvc.perform(get("/api/v1/posts/map-markers")
                        .param("swLat", "37.54").param("swLng", "126.91")
                        .param("neLat", "37.57").param("neLng", "126.94")
                        .param("zoom", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markers.length()").value(2))
                .andExpect(jsonPath("$.data.markers[0].count").value(12));
    }

    @Test
    @DisplayName("PO-11 위도가 없으면 COMMON_001")
    void countPosts_withoutLat_returnsCommon001() throws Exception {
        expectCommon001(mockMvc.perform(get("/api/v1/posts/count").param("lng", "126.9")));
    }

    private static void expectCommon001(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
