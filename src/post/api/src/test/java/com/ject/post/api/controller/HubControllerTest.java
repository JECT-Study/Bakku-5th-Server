package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.post.api.exception.PostExceptionHandler;
import com.ject.post.application.dto.HubResult;
import com.ject.post.application.dto.query.GetHubsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HubControllerTest {

    private final FakeQueryBus queryBus = new FakeQueryBus();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HubController(queryBus))
                .setControllerAdvice(new PostExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("거점 목록은 코드 · 화면 이름 · 중심 좌표 · 다각형을 받은 순서대로 돌려준다")
    void getHubs_returnsHubs() throws Exception {
        mockMvc.perform(get("/api/v1/hubs").requestAttr(LoginMember.ATTRIBUTE, 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hubs.length()").value(2))
                .andExpect(jsonPath("$.data.hubs[0].code").value("HONGDAE"))
                .andExpect(jsonPath("$.data.hubs[0].name").value("홍대"))
                .andExpect(jsonPath("$.data.hubs[0].center.lat").value(37.55622))
                .andExpect(jsonPath("$.data.hubs[0].center.lng").value(126.92338))
                .andExpect(jsonPath("$.data.hubs[0].polygon[0].lat").value(37.55695))
                .andExpect(jsonPath("$.data.hubs[1].code").value("GUKJEON"))
                .andExpect(jsonPath("$.data.hubs[1].name").value("국제전자센터"));

        assertEquals(new GetHubsQuery(), queryBus.received);
    }

    @Test
    @DisplayName("로그인 정보가 없으면 AUTH_001")
    void getHubs_withoutLogin_returnsAuth001() throws Exception {
        mockMvc.perform(get("/api/v1/hubs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    static class FakeQueryBus implements QueryBus {

        Query<?> received;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R send(Query<R> query) {
            received = query;
            return (R) List.of(
                    new HubResult("HONGDAE", new HubResult.Coordinate(37.55622, 126.92338),
                            List.of(new HubResult.Coordinate(37.55695, 126.92072))),
                    new HubResult("GUKJEON", new HubResult.Coordinate(37.48463, 127.01836),
                            List.of(new HubResult.Coordinate(37.48612, 127.01524))));
        }
    }
}
