package com.ject.notification.api.controller;

import com.ject.notification.api.exception.NotificationExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController())
                .setControllerAdvice(new NotificationExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("NT-1 알림 목록을 최신순 커서 형식으로 반환하고, 1차 알림은 채팅방으로 이동한다")
    void getNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].type").value("APPOINTMENT_PROPOSED"))
                .andExpect(jsonPath("$.data.content[0].target.type").value("CHAT_ROOM"))
                .andExpect(jsonPath("$.data.content[0].read").value(false))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-12-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("NT-1 size가 1보다 작으면 COMMON_001")
    void getNotifications_sizeZero_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("NT-2 안 읽은 알림 수를 반환한다")
    void getUnreadCount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    @DisplayName("NT-3 읽음 처리하면 200과 read=true를 반환한다")
    void read() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/9001/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(9001))
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    @DisplayName("NT-4 모두 읽음은 읽음 처리한 수를 반환한다")
    void readAll() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readCount").value(3));
    }
}
