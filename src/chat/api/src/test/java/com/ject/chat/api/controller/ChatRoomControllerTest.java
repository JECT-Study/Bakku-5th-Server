package com.ject.chat.api.controller;

import com.ject.chat.application.dto.ChatInputDisabledReason;
import com.ject.chat.application.dto.ChatRoomAccessStatus;
import com.ject.chat.application.dto.ChatRoomDetailResult;
import com.ject.chat.application.dto.ChatRoomLeaveImpact;
import com.ject.chat.application.dto.CounterpartDetail;
import com.ject.chat.application.dto.query.GetChatRoomDetailQuery;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class ChatRoomControllerTest {

    private final FakeQueryBus queryBus = new FakeQueryBus();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatRoomController(queryBus))
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("CH-2 로그인 회원과 방 ID로 실제 상세 Query를 보내 응답한다")
    void getChatRoom_sendsAuthenticatedQueryAndMapsResponse() throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms/5").requestAttr(LoginMember.ATTRIBUTE, 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value(5))
                .andExpect(jsonPath("$.data.status").value("READ_ONLY"))
                .andExpect(jsonPath("$.data.inputDisabledReason").value("COUNTERPART_RESTRICTED"))
                .andExpect(jsonPath("$.data.counterpart.nickname").value("하치와레러버"))
                .andExpect(jsonPath("$.data.notificationEnabled").value(false))
                .andExpect(jsonPath("$.data.leaveImpact").value("COMPLETE_EXCHANGE"))
                .andExpect(jsonPath("$.data.myLastReadMessageId").value(117));

        assertEquals(new GetChatRoomDetailQuery(5L, 101L), queryBus.received);
    }

    private static class FakeQueryBus implements QueryBus {
        Query<?> received;
        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Query<R> query) {
            received = query;
            return (R) new ChatRoomDetailResult(5L, 1L, ChatRoomAccessStatus.READ_ONLY,
                    ChatInputDisabledReason.COUNTERPART_RESTRICTED,
                    new CounterpartDetail(202L, "하치와레러버", 5, false), false,
                    ChatRoomLeaveImpact.COMPLETE_EXCHANGE, 117L);
        }
    }
}
