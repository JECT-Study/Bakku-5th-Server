package com.ject.notification.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.notification.api.exception.NotificationExceptionHandler;
import com.ject.notification.application.dto.NotificationPage;
import com.ject.notification.application.dto.NotificationPage.NotificationItem;
import com.ject.notification.application.service.NotificationQueryService;
import com.ject.notification.application.service.NotificationReadService;
import com.ject.notification.domain.exception.NotificationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private static final Long MEMBER_ID = 101L;
    private static final Instant CREATED_AT = Instant.parse("2026-12-01T00:00:00Z");

    private MockMvc mockMvc;
    private StubNotificationQueryService queryService;
    private StubNotificationReadService readService;

    @BeforeEach
    void setUp() {
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(CREATED_AT, ZoneId.of("Asia/Seoul")));
        queryService = new StubNotificationQueryService();
        readService = new StubNotificationReadService();
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, readService, serviceTime))
                .setControllerAdvice(new NotificationExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("NT-1 로그인한 회원의 알림 목록과 다음 커서를 반환한다")
    void getNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].notificationId").value(9001))
                .andExpect(jsonPath("$.data.content[0].type").value("EXCHANGE_REQUESTED"))
                .andExpect(jsonPath("$.data.content[0].title").value("하치와레러버"))
                .andExpect(jsonPath("$.data.content[0].body").value("[포켓몬 카드] 교환 요청이 왔어요"))
                .andExpect(jsonPath("$.data.content[0].target.type").value("CHAT_ROOM"))
                .andExpect(jsonPath("$.data.content[0].target.id").value(5))
                .andExpect(jsonPath("$.data.content[0].read").value(false))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-12-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.data.content[1].target").value(nullValue()))
                .andExpect(jsonPath("$.data.content[1].read").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("eyJpZCI6OTAwMH0"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        assertEquals(MEMBER_ID, queryService.requestedMemberId);
        assertEquals(20, queryService.requestedSize);
    }

    @Test
    @DisplayName("NT-1 받은 커서를 id로 풀어 넘긴다")
    void getNotifications_withCursor() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .param("cursor", "eyJpZCI6OTAwMH0"))
                .andExpect(status().isOk());
        assertEquals(9000L, queryService.requestedCursorId);
    }

    @Test
    @DisplayName("NT-1 커서 형식이 틀리면 COMMON_001")
    void getNotifications_invalidCursor_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .param("cursor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("NT-1 size가 1보다 작으면 COMMON_001")
    void getNotifications_sizeZero_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("NT-2 로그인한 회원의 안 읽은 알림 수를 반환한다")
    void getUnreadCount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
        assertEquals(MEMBER_ID, queryService.requestedMemberId);
    }

    @Test
    @DisplayName("NT-3 읽음 처리하면 200과 read=true를 반환한다")
    void read() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/9001/read").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(9001))
                .andExpect(jsonPath("$.data.read").value(true));
        assertEquals(List.of(MEMBER_ID, 9001L), readService.read);
    }

    @Test
    @DisplayName("NT-3 없는 알림 · 남의 알림이면 404 NOTIFICATION_001")
    void read_notFound_returnsNotification001() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/404/read").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_001"));
    }

    @Test
    @DisplayName("NT-4 모두 읽음은 로그인한 회원 기준으로 읽음 처리한 수를 반환한다")
    void readAll() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readCount").value(3));
        assertEquals(List.of(MEMBER_ID), readService.read);
    }

    /**
     * 404는 없는 알림.
     */
    private static class StubNotificationReadService extends NotificationReadService {

        List<Long> read;

        StubNotificationReadService() {
            super(null, null);
        }

        @Override
        public void read(Long memberId, Long notificationId) {
            if (notificationId == 404L) {
                throw new NotificationNotFoundException(notificationId);
            }
            read = List.of(memberId, notificationId);
        }

        @Override
        public long readAll(Long memberId) {
            read = List.of(memberId);
            return 3;
        }
    }

    private static class StubNotificationQueryService extends NotificationQueryService {

        Long requestedMemberId;
        Long requestedCursorId;
        Integer requestedSize;

        StubNotificationQueryService() {
            super(null, null);
        }

        @Override
        public NotificationPage getNotifications(Long memberId, Long cursorId, int size) {
            requestedMemberId = memberId;
            requestedCursorId = cursorId;
            requestedSize = size;
            return new NotificationPage(List.of(
                    new NotificationItem(9001L, NotificationType.EXCHANGE_REQUESTED, "하치와레러버",
                            "[포켓몬 카드] 교환 요청이 왔어요", NotificationTargetType.CHAT_ROOM, 5L, false, CREATED_AT),
                    new NotificationItem(9000L, NotificationType.AUTO_COMPLETE_SOON, "치이카와",
                            "내일 교환이 자동으로 완료돼요", null, null, true, CREATED_AT)),
                    9000L, true);
        }

        @Override
        public long getUnreadCount(Long memberId) {
            requestedMemberId = memberId;
            return 3;
        }
    }
}
