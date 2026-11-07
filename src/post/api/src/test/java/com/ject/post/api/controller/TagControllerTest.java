package com.ject.post.api.controller;

import com.ject.post.api.exception.PostExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TagControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TagController())
                .setControllerAdvice(new PostExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("PO-9 입력 글자가 들어간 태그를 추천한다")
    void suggestTags_returnsTags() throws Exception {
        mockMvc.perform(get("/api/v1/tags").param("keyword", "치이"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags[0]").value("치이카와"));
    }
}
