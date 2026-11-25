package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.user.api.exception.UserExceptionHandler;
import com.ject.user.api.ratelimit.IpRateLimiter;
import com.ject.user.api.ratelimit.RateLimitInterceptor;
import com.ject.user.application.dto.MarketingConsent;
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.dto.NicknameChange;
import com.ject.user.application.dto.NicknameAvailability;
import com.ject.user.application.service.MemberService;
import com.ject.user.application.service.NicknameService;
import com.ject.user.application.service.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest {

    private MockMvc mockMvc;
    private StubWithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-14T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        ServiceTime serviceTime = new ServiceTime(clock);
        withdrawalService = new StubWithdrawalService();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MemberController(new StubNicknameService(), new StubMemberService(), withdrawalService,
                                serviceTime))
                .setControllerAdvice(new UserExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .addMappedInterceptors(new String[]{"/api/v1/members/nickname-availability"},
                        new RateLimitInterceptor(new IpRateLimiter(30, Duration.ofMinutes(1), serviceTime)))
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
    @CsvSource({"가, INVALID_LENGTH", "닉 네임, INVALID_FORMAT", "쿠로미덕후, DUPLICATE"})
    @DisplayName("MB-1 사용할 수 없으면 에러 대신 available=false와 이유를 준다")
    void checkNickname_unavailable(String nickname, String reason) throws Exception {
        mockMvc.perform(get("/api/v1/members/nickname-availability").param("nickname", nickname))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.reason").value(reason));
    }

    @Test
    @DisplayName("MB-1 같은 IP에서 1분에 30회를 넘으면 COMMON_003")
    void checkNickname_tooManyRequests_returnsCommon003() throws Exception {
        // given
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(nicknameCheckFrom("203.0.113.1")).andExpect(status().isOk());
        }

        // when & then
        mockMvc.perform(nicknameCheckFrom("203.0.113.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("COMMON_003"));
        mockMvc.perform(nicknameCheckFrom("203.0.113.2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MB-1 X-Forwarded-For를 매번 바꿔 보내도 접속 IP 기준으로 센다")
    void checkNickname_forgedForwardedFor_countedByRemoteAddr() throws Exception {
        // given
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(nicknameCheckFrom("203.0.113.1").header("X-Forwarded-For", "198.51.100." + i))
                    .andExpect(status().isOk());
        }

        // when & then
        mockMvc.perform(nicknameCheckFrom("203.0.113.1").header("X-Forwarded-For", "198.51.100.99"))
                .andExpect(status().isTooManyRequests());
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
    @DisplayName("MB-2 로그인한 회원의 내 정보를 주고 구현 전 · 출시 후 필드는 null이다")
    void getMe_returnsFirstReleaseFields() throws Exception {
        mockMvc.perform(get("/api/v1/members/me").requestAttr(LoginMember.ATTRIBUTE, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.nickname").value("쿠로미덕후"))
                .andExpect(jsonPath("$.data.provider").value("KAKAO"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.completedCount").value(12))
                .andExpect(jsonPath("$.data.birthDate").value("2001-03-15"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-11-01T12:00:00+09:00"))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("nicknameChangeableAt"), nullValue())))
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
    @DisplayName("MB-6 로그인한 회원을 탈퇴시키고 사유를 넘긴다")
    void withdraw_withReason() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me")
                        .requestAttr(LoginMember.ATTRIBUTE, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"HARD_TO_FIND_PARTNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(List.of("7:HARD_TO_FIND_PARTNER"), withdrawalService.calls);
    }

    @Test
    @DisplayName("MB-6 본문 없이도 탈퇴할 수 있다")
    void withdraw_withoutBody() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me").requestAttr(LoginMember.ATTRIBUTE, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(List.of("7:null"), withdrawalService.calls);
    }

    @Test
    @DisplayName("MB-6 이유가 목록에 없으면 COMMON_002")
    void withdraw_unknownReason_returnsCommon002() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me")
                        .requestAttr(LoginMember.ATTRIBUTE, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"BORED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_002"));
    }

    @Test
    @DisplayName("MB-7 바꾼 값과 변경 일시를 반환한다")
    void updateMarketingConsent() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/marketing-consent")
                        .requestAttr(LoginMember.ATTRIBUTE, 7L)
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
                        .requestAttr(LoginMember.ATTRIBUTE, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("MB-3 로그인한 회원의 닉네임을 바꾸고 다음 변경 가능 시각을 반환한다")
    void updateNickname() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/nickname").requestAttr(LoginMember.ATTRIBUTE, 7L)
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
        mockMvc.perform(get("/api/v1/members/me").requestAttr(LoginMember.ATTRIBUTE, 7L))
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

    private static MockHttpServletRequestBuilder nicknameCheckFrom(String ip) {
        return get("/api/v1/members/nickname-availability").param("nickname", "하치와레러버")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }

    /**
     * 받은 회원 id를 그대로 담아 내 정보를 주는 회원 서비스.
     */
    private static class StubMemberService extends MemberService {

        StubMemberService() {
            super(null, null);
        }

        @Override
        public MyInfo getMyInfo(Long memberId) {
            return new MyInfo(memberId, "쿠로미덕후", "KAKAO", LocalDate.of(2001, 3, 15), "FEMALE", 12, null, false,
                    Instant.parse("2026-11-01T03:00:00Z"));
        }

        @Override
        public MarketingConsent updateMarketingConsent(Long memberId, boolean agreed) {
            return new MarketingConsent(agreed, Instant.parse("2026-11-27T05:30:00Z"));
        }

        @Override
        public NicknameChange changeNickname(Long memberId, String value) {
            if (memberId != 7L) {
                throw new IllegalStateException("로그인한 회원 id가 넘어와야 한다");
            }
            return new NicknameChange(value, Instant.parse("2026-12-27T05:30:00Z"));
        }
    }

    /**
     * 받은 회원 id와 사유를 남기는 탈퇴 서비스.
     */
    private static class StubWithdrawalService extends WithdrawalService {

        private final List<String> calls = new ArrayList<>();

        StubWithdrawalService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public void withdraw(Long memberId, String reason) {
            calls.add(memberId + ":" + reason);
        }
    }

    /**
     * "가"는 글자 수 오류, 공백이 있으면 형식 오류, "쿠로미덕후"는 중복으로 보는 닉네임 서비스.
     */
    private static class StubNicknameService extends NicknameService {

        StubNicknameService() {
            super(null);
        }

        @Override
        public NicknameAvailability checkAvailability(String value) {
            if (value.length() < 2) {
                return NicknameAvailability.unusable("INVALID_LENGTH");
            }
            if (value.contains(" ")) {
                return NicknameAvailability.unusable("INVALID_FORMAT");
            }
            if (value.equals("쿠로미덕후")) {
                return NicknameAvailability.unusable("DUPLICATE");
            }
            return NicknameAvailability.usable();
        }
    }
}
