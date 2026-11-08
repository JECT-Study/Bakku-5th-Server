package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController())
                .setControllerAdvice(new UserExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("RP-1 신고하면 201과 신고 ID를 반환한다")
    void report_returns201() throws Exception {
        report("{\"targetType\": \"CHAT_ROOM\", \"targetId\": 5, \"reason\": \"EXTERNAL_TRADE\", \"detail\": null}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId").value(31));
    }

    @Test
    @DisplayName("RP-1 기타 사유는 내용을 입력하면 신고할 수 있다")
    void report_otherWithDetail_returns201() throws Exception {
        report("{\"targetType\": \"POST\", \"targetId\": 10, \"reason\": \"OTHER\", \"detail\": \"연락처를 올렸어요\"}")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RP-1 기타 사유인데 내용이 없으면 COMMON_001")
    void report_otherWithoutDetail_returnsCommon001() throws Exception {
        report("{\"targetType\": \"POST\", \"targetId\": 10, \"reason\": \"OTHER\", \"detail\": \" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("RP-1 내용이 300자를 넘으면 COMMON_001")
    void report_detailOver300_returnsCommon001() throws Exception {
        report("{\"targetType\": \"POST\", \"targetId\": 10, \"reason\": \"OTHER\", \"detail\": \"%s\"}".formatted("가".repeat(301)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private ResultActions report(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
