package com.ject.chat.api.controller;

import com.ject.chat.api.exception.ChatExceptionHandler;
import com.ject.chat.application.dto.ChatMessagePage;
import com.ject.chat.application.dto.CardMessageContentResult;
import com.ject.chat.application.dto.ChatMessageCardType;
import com.ject.chat.application.dto.ChatMessageResult;
import com.ject.chat.application.dto.ChatMessageSystemCode;
import com.ject.chat.application.dto.ChatMessageSystemLinkType;
import com.ject.chat.application.dto.ChatMessageType;
import com.ject.chat.application.dto.ImageMessageContentResult;
import com.ject.chat.application.dto.SystemMessageContentResult;
import com.ject.chat.application.dto.TextMessageContentResult;
import com.ject.chat.application.dto.query.GetChatMessagesQuery;
import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class ChatMessageControllerTest {

    private final FakeQueryBus queryBus = new FakeQueryBus();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatMessageController(queryBus))
                .setControllerAdvice(new ChatExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("CH-3 로그인 회원과 조회 조건을 Query로 보내 TEXT 응답을 매핑한다")
    void getMessages_sendsAuthenticatedQueryAndMapsText() throws Exception {
        String cursor = ChatMessageCursor.encode(90L);

        mockMvc.perform(get("/api/v1/chat-rooms/5/messages")
                        .requestAttr(LoginMember.ATTRIBUTE, 101L)
                        .param("cursor", cursor)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].messageId").value(121))
                .andExpect(jsonPath("$.data.content[0].type").value("TEXT"))
                .andExpect(jsonPath("$.data.content[0].content.text").value("안녕하세요"))
                .andExpect(jsonPath("$.data.content[0].sentAt").value("2026-11-28T15:00:00+09:00"))
                .andExpect(jsonPath("$.data.nextCursor").value(ChatMessageCursor.encode(80L)))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        assertEquals(new GetChatMessagesQuery(5L, 101L, 90L, 20, null), queryBus.received);
    }

    @Test
    @DisplayName("CH-3 잘못된 cursor는 COMMON_001 400")
    void getMessages_invalidCursor_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms/5/messages")
                        .requestAttr(LoginMember.ATTRIBUTE, 101L)
                        .param("cursor", "not-base64"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("CH-3 cursor와 after 동시 입력은 COMMON_001 400")
    void getMessages_cursorAndAfter_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms/5/messages")
                        .requestAttr(LoginMember.ATTRIBUTE, 101L)
                        .param("cursor", ChatMessageCursor.encode(90L))
                        .param("after", "91"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("CH-3 0 이하 cursor ID는 COMMON_001 400")
    void getMessages_nonPositiveCursorId_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms/5/messages")
                        .requestAttr(LoginMember.ATTRIBUTE, 101L)
                        .param("cursor", ChatMessageCursor.encode(0L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("CH-3 IMAGE SYSTEM CARD 내용을 기존 다형 응답으로 매핑한다")
    void getMessages_mapsImageSystemAndCardContents() throws Exception {
        queryBus.page = new ChatMessagePage(List.of(
                new ChatMessageResult(1L, 5L, 101L, ChatMessageType.IMAGE,
                        new ImageMessageContentResult(List.of("https://cdn/a", "https://cdn/b")), "image-client",
                        Instant.parse("2026-11-28T06:00:00Z")),
                new ChatMessageResult(2L, 5L, null, ChatMessageType.SYSTEM,
                        new SystemMessageContentResult(ChatMessageSystemCode.ATTACHED_POST_UPDATED, "수정했어요",
                                ChatMessageSystemLinkType.POST, 21L), null,
                        Instant.parse("2026-11-28T06:01:00Z")),
                new ChatMessageResult(3L, 5L, null, ChatMessageType.CARD,
                        new CardMessageContentResult(ChatMessageCardType.APPOINTMENT_PROPOSAL, 31L), null,
                        Instant.parse("2026-11-28T06:02:00Z"))), null, false);

        mockMvc.perform(get("/api/v1/chat-rooms/5/messages")
                        .requestAttr(LoginMember.ATTRIBUTE, 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content.imageUrls[1]").value("https://cdn/b"))
                .andExpect(jsonPath("$.data.content[1].content.code").value("ATTACHED_POST_UPDATED"))
                .andExpect(jsonPath("$.data.content[1].content.link.type").value("POST"))
                .andExpect(jsonPath("$.data.content[1].content.link.id").value(21))
                .andExpect(jsonPath("$.data.content[2].content.cardType").value("APPOINTMENT_PROPOSAL"))
                .andExpect(jsonPath("$.data.content[2].content.targetId").value(31));
    }

    private static class FakeQueryBus implements QueryBus {
        Query<?> received;
        ChatMessagePage page = new ChatMessagePage(List.of(new ChatMessageResult(
                121L, 5L, 101L, ChatMessageType.TEXT,
                new TextMessageContentResult("안녕하세요", false), "client-1",
                Instant.parse("2026-11-28T06:00:00Z"))), 80L, true);
        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Query<R> query) {
            received = query;
            return (R) page;
        }
    }
}
