package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberController())
                .setControllerAdvice(new UserExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("MB-1 사용할 수 있는 닉네임이면 available=true, reason=null")
    void checkNickname_available() throws Exception {
        mockMvc.perform(get("/api/v1/members/nickname-availability").param("nickname", "하치와레러버"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("reason"), nullValue())))
                .andExpect(jsonPath("$.data.bannedWordDetected").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"가", "열한글자닉네임입니다요", "닉 네임", "닉네임!", "ㅋㅋ"})
    @DisplayName("MB-1 형식이 틀리면 에러 대신 INVALID_FORMAT")
    void checkNickname_invalidFormat(String nickname) throws Exception {
        mockMvc.perform(get("/api/v1/members/nickname-availability").param("nickname", nickname))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.reason").value("INVALID_FORMAT"));
    }

    @Test
    @DisplayName("MB-1 중복은 영문 대소문자를 구분하지 않는다")
    void checkNickname_duplicate() throws Exception {
        mockMvc.perform(get("/api/v1/members/nickname-availability").param("nickname", "쿠로미덕후"))
                .andExpect(jsonPath("$.data.reason").value("DUPLICATE"));
    }

    @Test
    @DisplayName("MB-1 nickname이 없으면 COMMON_001")
    void checkNickname_missing_returnsCommon001() throws Exception {
        mockMvc.perform(get("/api/v1/members/nickname-availability"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
    }

    @Test
    @DisplayName("MB-2 내 정보는 1차 필드를 채우고 2차 필드는 null이다")
    void getMe_returnsFirstReleaseFields() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.completedCount").value(12))
                .andExpect(jsonPath("$.data.birthDate").value("2001-03-15"))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("mannerScore"), nullValue())))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("noShow"), nullValue())));
    }

    @Test
    @DisplayName("MB-5 진행 중인 교환 수를 반환한다")
    void getWithdrawalNotice() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/withdrawal-notice"))
                .andExpect(jsonPath("$.data.inProgressExchangeCount").value(1));
    }

    @Test
    @DisplayName("MB-6 이유 없이도 탈퇴할 수 있다")
    void withdraw_withoutReason() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("MB-6 이유가 목록에 없으면 COMMON_002")
    void withdraw_unknownReason_returnsCommon002() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"BORED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @DisplayName("MB-7 바꾼 값과 변경 일시를 반환한다")
    void updateMarketingConsent() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/marketing-consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreed").value(true))
                .andExpect(jsonPath("$.data.changedAt").value("2026-11-27T14:30:00+09:00"));
    }

    @Test
    @DisplayName("MB-7 agreed가 없으면 COMMON_001")
    void updateMarketingConsent_missingAgreed_returnsCommon001() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/marketing-consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("MB-3 닉네임을 바꾸면 다음 변경 가능 시각은 30일 뒤다")
    void updateNickname() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"하치와레러버\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("하치와레러버"))
                .andExpect(jsonPath("$.data.nicknameChangeableAt").value("2026-12-27T14:30:00+09:00"));
    }

    @Test
    @DisplayName("MB-4 다른 회원 프로필은 isMe=false, 내 ID면 isMe=true")
    void getProfile() throws Exception {
        mockMvc.perform(get("/api/v1/members/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(202))
                .andExpect(jsonPath("$.data.isMe").value(false));
        mockMvc.perform(get("/api/v1/members/101"))
                .andExpect(jsonPath("$.data.isMe").value(true));
    }

    @Test
    @DisplayName("MB-4 /members/me는 다른 회원 프로필이 아니라 내 정보로 간다")
    void getMe_notMatchedAsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.birthDate").value("2001-03-15"));
    }

    @Test
    @DisplayName("MB-8 노쇼 기록을 커서 형식으로 반환한다")
    void getNoShows() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/no-shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
}
