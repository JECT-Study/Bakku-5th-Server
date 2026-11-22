package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.user.api.exception.UserExceptionHandler;
import com.ject.user.application.dto.BlockResult;
import com.ject.user.application.dto.BlockedMemberPage;
import com.ject.user.application.dto.BlockedMemberPage.BlockedMember;
import com.ject.user.application.service.BlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlockControllerTest {

    private static final Long MEMBER_ID = 7L;
    private static final Instant BLOCKED_AT = Instant.parse("2026-11-22T05:30:00Z");

    private MockMvc mockMvc;
    private StubBlockService blockService;

    @BeforeEach
    void setUp() {
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(BLOCKED_AT, ZoneId.of("Asia/Seoul")));
        blockService = new StubBlockService();
        mockMvc = MockMvcBuilders.standaloneSetup(new BlockController(blockService, serviceTime))
                .setControllerAdvice(new UserExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("BL-1 새로 차단하면 201과 차단한 회원 · 시각을 반환한다")
    void block_returns201() throws Exception {
        mockMvc.perform(blockRequest("{\"userId\": 202}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(202))
                .andExpect(jsonPath("$.data.blockedAt").value("2026-11-22T14:30:00+09:00"));
    }

    @Test
    @DisplayName("BL-1 이미 차단한 회원이면 200과 기존 차단 시각을 반환한다")
    void block_alreadyBlocked_returns200() throws Exception {
        mockMvc.perform(blockRequest("{\"userId\": 203}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(203))
                .andExpect(jsonPath("$.data.blockedAt").value("2026-11-21T14:30:00+09:00"));
    }

    @Test
    @DisplayName("BL-1 userId가 없으면 COMMON_001")
    void block_missingUserId_returnsCommon001() throws Exception {
        mockMvc.perform(blockRequest("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("BL-2 로그인한 회원의 차단 목록과 다음 커서를 반환한다")
    void getBlocks() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].userId").value(202))
                .andExpect(jsonPath("$.data.content[0].nickname").value("하치와레러버"))
                .andExpect(jsonPath("$.data.content[0].blockedAt").value("2026-11-22T14:30:00+09:00"))
                .andExpect(jsonPath("$.data.nextCursor").value("eyJpZCI6MTB9"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        assertEquals(MEMBER_ID, blockService.requestedBlockerId);
        assertEquals(20, blockService.requestedSize);
    }

    @Test
    @DisplayName("BL-2 받은 커서를 id로 풀어 넘긴다")
    void getBlocks_withCursor() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .param("cursor", "eyJpZCI6MTB9"))
                .andExpect(status().isOk());
        assertEquals(10L, blockService.requestedCursorId);
    }

    @Test
    @DisplayName("BL-2 커서 형식이 틀리면 COMMON_001")
    void getBlocks_invalidCursor_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .param("cursor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("BL-2 size가 50을 넘으면 COMMON_001")
    void getBlocks_sizeOver50_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks").requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("BL-3 차단 해제하면 성공만 반환한다")
    void unblock() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/blocks/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private static MockHttpServletRequestBuilder blockRequest(String body) {
        return post("/api/v1/members/me/blocks")
                .requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /**
     * 202는 새로 차단, 203은 이미 차단한 회원.
     */
    private static class StubBlockService extends BlockService {

        Long requestedBlockerId;
        Long requestedCursorId;
        Integer requestedSize;

        StubBlockService() {
            super(null, null, null, null);
        }

        @Override
        public BlockResult block(Long blockerId, Long blockedId) {
            if (blockedId == 203L) {
                return new BlockResult(blockedId, BLOCKED_AT.minusSeconds(86400), false);
            }
            return new BlockResult(blockedId, BLOCKED_AT, true);
        }

        @Override
        public BlockedMemberPage getBlocks(Long blockerId, Long cursorId, int size) {
            requestedBlockerId = blockerId;
            requestedCursorId = cursorId;
            requestedSize = size;
            return new BlockedMemberPage(List.of(new BlockedMember(202L, "하치와레러버", BLOCKED_AT)), 10L, true);
        }
    }
}
