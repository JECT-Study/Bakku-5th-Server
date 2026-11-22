package com.ject.user.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.user.api.exception.UserExceptionHandler;
import com.ject.user.application.dto.ReportCommand;
import com.ject.user.application.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerTest {

    private static final Long MEMBER_ID = 7L;

    private MockMvc mockMvc;
    private StubReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new StubReportService();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .setControllerAdvice(new UserExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("RP-1 신고하면 201과 신고 ID를 반환한다")
    void report_returns201() throws Exception {
        report("{\"targetType\": \"CHAT_ROOM\", \"targetId\": 5, \"reason\": \"EXTERNAL_TRADE\", \"detail\": null}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId").value(31));
        assertEquals(new ReportCommand(MEMBER_ID, "CHAT_ROOM", 5L, "EXTERNAL_TRADE", null), reportService.received);
    }

    @Test
    @DisplayName("RP-1 채팅 메시지 신고는 2차라 COMMON_001")
    void report_chatMessage_returnsCommon001() throws Exception {
        report("{\"targetType\": \"CHAT_MESSAGE\", \"targetId\": 5, \"reason\": \"EXTERNAL_TRADE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
        assertNull(reportService.received);
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
                .requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static class StubReportService extends ReportService {

        ReportCommand received;

        StubReportService() {
            super(null, null, null);
        }

        @Override
        public Long report(ReportCommand command) {
            received = command;
            return 31L;
        }
    }
}
