package com.ject.user.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.user.api.exception.UserExceptionHandler;
import com.ject.user.application.dto.RegisterDeviceCommand;
import com.ject.user.application.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerTest {

    private MockMvc mockMvc;
    private StubDeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new StubDeviceService();
        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceService))
                .setControllerAdvice(new UserExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("DV-1 로그인한 회원의 기기로 등록하고 성공만 반환한다")
    void registerDevice() throws Exception {
        register("""
                {"deviceId": "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f", "pushToken": "fcm-token-value", "platform": "IOS", "appVersion": "1.0.0"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(List.of(new RegisterDeviceCommand("6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f", "fcm-token-value",
                "IOS", "1.0.0")), deviceService.commands);
        assertEquals(List.of(101L), deviceService.memberIds);
    }

    @Test
    @DisplayName("DV-1 pushToken이 없으면 COMMON_001")
    void registerDevice_missingPushToken_returnsCommon001() throws Exception {
        register("{\"deviceId\": \"6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f\", \"platform\": \"ANDROID\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("pushToken"));
        assertTrue(deviceService.commands.isEmpty());
    }

    @Test
    @DisplayName("DV-1 기기 ID가 100자를 넘으면 COMMON_001")
    void registerDevice_tooLongDeviceId_returnsCommon001() throws Exception {
        register("{\"deviceId\": \"%s\", \"pushToken\": \"fcm\", \"platform\": \"ANDROID\"}".formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("deviceId"));
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(put("/api/v1/members/me/devices")
                .requestAttr(LoginMember.ATTRIBUTE, 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /**
     * 받은 회원 id와 명령을 남기는 기기 서비스.
     */
    private static class StubDeviceService extends DeviceService {

        private final List<Long> memberIds = new ArrayList<>();
        private final List<RegisterDeviceCommand> commands = new ArrayList<>();

        StubDeviceService() {
            super(null);
        }

        @Override
        public void register(Long memberId, RegisterDeviceCommand command) {
            memberIds.add(memberId);
            commands.add(command);
        }
    }
}
