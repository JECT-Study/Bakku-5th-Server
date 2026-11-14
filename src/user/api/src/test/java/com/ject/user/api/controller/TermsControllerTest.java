package com.ject.user.api.controller;

import com.ject.user.api.config.TermsProperties;
import com.ject.user.application.service.TermsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermsControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new TermsController(new TermsService(), new TermsProperties("https://bakku.test")))
            .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
            .build();

    @Test
    @DisplayName("TM-1 가입 필수 3개 · 선택 1개 · 보기만 1개 약관을 반환하고, 만 14세 확인은 본문 URL이 없다")
    void getTerms() throws Exception {
        mockMvc.perform(get("/api/v1/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(5)))
                .andExpect(jsonPath("$.data.content[0].termsType").value("SERVICE"))
                .andExpect(jsonPath("$.data.content[0].signupConsent").value("REQUIRED"))
                .andExpect(jsonPath("$.data.content[0].version").value("1.0"))
                .andExpect(jsonPath("$.data.content[0].url").value("https://bakku.test/terms/service/1.0"))
                .andExpect(jsonPath("$.data.content[0].effectiveAt").value("2026-11-27"))
                .andExpect(jsonPath("$.data.content[2].termsType").value("AGE_14"))
                .andExpect(jsonPath("$.data.content[2]", hasEntry(equalTo("url"), nullValue())))
                .andExpect(jsonPath("$.data.content[3].signupConsent").value("OPTIONAL"))
                .andExpect(jsonPath("$.data.content[4].signupConsent").value("NONE"));
    }
}
