package com.ject.notification.api.controller;

import com.ject.notification.api.exception.NotificationExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationSettingControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationSettingController())
                .setControllerAdvice(new NotificationExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("NT-5 토글 6종을 반환한다")
    void getSettings() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/notification-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings", hasSize(6)))
                .andExpect(jsonPath("$.data.settings[5].type").value("MARKETING"))
                .andExpect(jsonPath("$.data.settings[5].enabled").value(false));
    }

    @Test
    @DisplayName("NT-6 바꾼 항목과 값을 반환한다")
    void updateSetting() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"APPOINTMENT\", \"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("APPOINTMENT"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("NT-6 type 값이 틀리면 COMMON_002")
    void updateSetting_unknownType_returnsCommon002() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"SOUND\", \"enabled\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }
}
