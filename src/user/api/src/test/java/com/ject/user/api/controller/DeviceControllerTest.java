package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController())
                .setControllerAdvice(new UserExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("DV-1 푸시 토큰을 등록하면 성공만 반환한다")
    void registerDevice() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId": "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f", "pushToken": "fcm-token-value", "platform": "IOS", "appVersion": "1.0.0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DV-1 pushToken이 없으면 COMMON_001")
    void registerDevice_missingPushToken_returnsCommon001() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\": \"6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f\", \"platform\": \"ANDROID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("pushToken"));
    }
}
