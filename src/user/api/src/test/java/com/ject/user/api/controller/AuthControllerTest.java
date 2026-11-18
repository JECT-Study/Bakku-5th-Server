package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.user.api.exception.UserExceptionHandler;
import com.ject.user.application.dto.AccessToken;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.dto.SignupCommand;
import com.ject.user.application.dto.SignupToken;
import com.ject.user.application.dto.SocialLoginResult;
import com.ject.user.application.exception.InvalidRefreshTokenException;
import com.ject.user.application.exception.RejoinNotAvailableException;
import com.ject.user.application.service.LogoutService;
import com.ject.user.application.service.SignupService;
import com.ject.user.application.service.SocialLoginService;
import com.ject.user.application.service.TokenRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final String DEVICE_ID = "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f";

    private static final String SIGNUP_BODY = """
            {
              "signupToken": "eyJhbGciOiJIUzI1NiJ9.signup",
              "deviceId": "%s",
              "agreements": [
                {"termsType": "SERVICE", "version": "1.0", "agreed": true},
                {"termsType": "PRIVACY_COLLECTION", "version": "1.0", "agreed": true},
                {"termsType": "AGE_14", "version": "1.0", "agreed": true},
                {"termsType": "MARKETING", "version": "1.0", "agreed": false}
              ],
              "nickname": "쿠로미덕후",
              "birthDate": "2001-03-15",
              "gender": null
            }
            """.formatted(DEVICE_ID);

    private MockMvc mockMvc;
    private StubLogoutService logoutService;

    @BeforeEach
    void setUp() {
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        logoutService = new StubLogoutService();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new StubSocialLoginService(), new StubSignupService(),
                        new StubTokenRefreshService(), logoutService, serviceTime))
                .setControllerAdvice(new UserExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("AU-1 기존 회원이면 LOGIN과 토큰 · 회원을 반환하고 가입 토큰은 null이다")
    void login_existingMember_returnsTokens() throws Exception {
        login("kakao", "{\"socialAccessToken\": \"kakao-token\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result").value("LOGIN"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").value("2026-11-15T13:00:00+09:00"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.member.userId").value(101))
                .andExpect(jsonPath("$.data.member.nickname").value("쿠로미덕후"))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("signupToken"), nullValue())));
    }

    @Test
    @DisplayName("AU-1 처음 로그인하면 SIGNUP_REQUIRED와 가입 토큰 · 자동 채우기 값을 반환하고 토큰은 null이다")
    void login_newMember_returnsSignupToken() throws Exception {
        login("naver", "{\"socialAccessToken\": \"new-naver-token\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.data.signupToken").value("signup-token"))
                .andExpect(jsonPath("$.data.signupTokenExpiresAt").value("2026-11-22T12:00:00+09:00"))
                .andExpect(jsonPath("$.data.prefill.birthDate").value("2001-03-15"))
                .andExpect(jsonPath("$.data.prefill.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("accessToken"), nullValue())));
    }

    @Test
    @DisplayName("AU-1 탈퇴 후 7일 안이면 MEMBER_002와 data.rejoinAvailableAt을 반환한다")
    void login_withdrawnWithin7Days_returnsMember002() throws Exception {
        login("kakao", "{\"socialAccessToken\": \"withdrawn-token\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_002"))
                .andExpect(jsonPath("$.data.rejoinAvailableAt").value("2026-11-22T12:00:00+09:00"));
    }

    @Test
    @DisplayName("AU-1 Apple인데 authorizationCode가 없으면 COMMON_001")
    void login_appleWithoutAuthorizationCode_returnsCommon001() throws Exception {
        login("apple", "{\"identityToken\": \"eyJraWQiOiJBSURPUEsxIn0.apple\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("authorizationCode"));
    }

    @Test
    @DisplayName("AU-1 지원하지 않는 provider면 COMMON_001")
    void login_unknownProvider_returnsCommon001() throws Exception {
        login("google", "{\"socialAccessToken\": \"token\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("AU-2 가입하면 201과 토큰 · 입력한 닉네임을 반환한다")
    void signup_returns201WithTokens() throws Exception {
        post("/api/v1/auth/signup", SIGNUP_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.member.nickname").value("쿠로미덕후"));
    }

    @Test
    @DisplayName("AU-2 생년월일이 없으면 COMMON_001")
    void signup_withoutBirthDate_returnsCommon001() throws Exception {
        post("/api/v1/auth/signup", SIGNUP_BODY.replace("\"birthDate\": \"2001-03-15\",", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"));
    }

    @Test
    @DisplayName("AU-3 재발급하면 새 토큰 · 만료 시각 · 회원을 반환한다")
    void refresh_returnsTokens() throws Exception {
        post("/api/v1/auth/token/refresh", "{\"refreshToken\": \"refresh-token\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").value("2026-11-15T13:00:00+09:00"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.member.userId").value(101));
    }

    @Test
    @DisplayName("AU-3 무효한 리프레시 토큰이면 401 AUTH_004")
    void refresh_invalidToken_returnsAuth004() throws Exception {
        post("/api/v1/auth/token/refresh", "{\"refreshToken\": \"invalid\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    @DisplayName("AU-3 이미 재발급된 토큰이면 409 AUTH_005")
    void refresh_alreadyReissued_returnsAuth005() throws Exception {
        post("/api/v1/auth/token/refresh", "{\"refreshToken\": \"reissued\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    @DisplayName("AU-3 리프레시 토큰이 없으면 COMMON_001")
    void refresh_withoutToken_returnsCommon001() throws Exception {
        post("/api/v1/auth/token/refresh", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
    }

    @Test
    @DisplayName("AU-4 로그인한 회원 · 리프레시 토큰 · 기기 ID로 로그아웃하고 성공만 반환한다")
    void logout_returnsSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .requestAttr(LoginMember.ATTRIBUTE, 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"refresh-token\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$", hasEntry(equalTo("data"), nullValue())));
        assertEquals(List.of("101:refresh-token:" + DEVICE_ID), logoutService.calls);
    }

    private ResultActions login(String provider, String body) throws Exception {
        return post("/api/v1/auth/" + provider + "/login", body);
    }

    private ResultActions post(String uri, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /**
     * 토큰이 new로 시작하면 가입 필요, withdrawn으로 시작하면 탈퇴 7일 안, 그 밖에는 기존 회원으로 보는 로그인 서비스.
     */
    private static class StubSocialLoginService extends SocialLoginService {

        StubSocialLoginService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public SocialLoginResult login(String provider, String socialAccessToken, String deviceId) {
            if (socialAccessToken.startsWith("new")) {
                return new SocialLoginResult(null, new SignupToken("signup-token", NOW.plus(Duration.ofDays(7))),
                        LocalDate.of(2001, 3, 15), "FEMALE");
            }
            if (socialAccessToken.startsWith("withdrawn")) {
                throw new RejoinNotAvailableException(OffsetDateTime.parse("2026-11-22T12:00:00+09:00"));
            }
            return SocialLoginResult.login(new LoginTokens(new AccessToken("access-token", NOW.plusSeconds(3600)),
                    "refresh-token", 101L, "쿠로미덕후"));
        }
    }

    /**
     * "invalid"는 무효, "reissued"는 이미 재발급됨, 그 밖에는 101번 회원에게 새 토큰을 주는 재발급 서비스.
     */
    private static class StubTokenRefreshService extends TokenRefreshService {

        StubTokenRefreshService() {
            super(null, null, null, null);
        }

        @Override
        public LoginTokens refresh(String refreshToken) {
            if (refreshToken.equals("invalid")) {
                throw InvalidRefreshTokenException.invalid();
            }
            if (refreshToken.equals("reissued")) {
                throw InvalidRefreshTokenException.alreadyReissued();
            }
            return new LoginTokens(new AccessToken("access-token", NOW.plusSeconds(3600)), "new-refresh-token", 101L,
                    "쿠로미덕후");
        }
    }

    /**
     * 받은 값을 "{회원 id}:{리프레시 토큰}:{기기 ID}"로 남기는 로그아웃 서비스.
     */
    private static class StubLogoutService extends LogoutService {

        private final List<String> calls = new ArrayList<>();

        StubLogoutService() {
            super(null, null);
        }

        @Override
        public void logout(Long memberId, String refreshToken, String deviceId) {
            calls.add(memberId + ":" + refreshToken + ":" + deviceId);
        }
    }

    /**
     * 받은 닉네임으로 101번 회원을 만든 것처럼 토큰을 주는 가입 서비스.
     */
    private static class StubSignupService extends SignupService {

        StubSignupService() {
            super(null, null, null, null, null);
        }

        @Override
        public LoginTokens signup(SignupCommand command) {
            return new LoginTokens(new AccessToken("access-token", NOW.plusSeconds(3600)), "refresh-token", 101L,
                    command.nickname());
        }
    }
}
