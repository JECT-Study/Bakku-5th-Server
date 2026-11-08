package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppVersionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AppVersionController())
                .setControllerAdvice(new UserExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("AP-1 플랫폼별 최소 버전과 스토어 URL을 반환한다")
    void getVersion() throws Exception {
        mockMvc.perform(get("/api/v1/app/version").param("platform", "ANDROID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minimumVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.storeUrl", startsWith("https://play.google.com")));
    }

    @Test
    @DisplayName("AP-1 platform 값이 틀리면 COMMON_001")
    void getVersion_unknownPlatform_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/app/version").param("platform", "WINDOWS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
