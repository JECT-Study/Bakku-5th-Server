package com.ject.user.api.controller;

import com.ject.user.api.exception.UserExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController())
                .setControllerAdvice(new UserExceptionHandler())
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
                .andExpect(jsonPath("$.data.accessToken").value("eyJhbGciOiJIUzI1NiJ9.access"))
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").value("2026-11-27T15:30:00+09:00"))
                .andExpect(jsonPath("$.data.member.userId").value(101))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("signupToken"), nullValue())));
    }

    @Test
    @DisplayName("AU-1 처음 로그인하면 SIGNUP_REQUIRED와 가입 토큰을 반환하고 토큰은 null이다")
    void login_newMember_returnsSignupToken() throws Exception {
        login("naver", "{\"socialAccessToken\": \"new-naver-token\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.data.signupToken").value("eyJhbGciOiJIUzI1NiJ9.signup"))
                .andExpect(jsonPath("$.data.signupTokenExpiresAt").value("2026-12-04T14:30:00+09:00"))
                .andExpect(jsonPath("$.data.prefill.birthDate").value("2001-03-15"))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("accessToken"), nullValue())));
    }

    @Test
    @DisplayName("AU-1 Apple은 identityToken과 authorizationCode로 로그인한다")
    void login_apple_returnsTokens() throws Exception {
        login("apple", """
                {"identityToken": "eyJraWQiOiJBSURPUEsxIn0.apple", "authorizationCode": "c1a2b3c4d5.0.apple", "deviceId": "%s"}
                """.formatted(DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("LOGIN"));
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
                .andExpect(jsonPath("$.data.refreshToken").value("eyJhbGciOiJIUzI1NiJ9.refresh"))
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
    @DisplayName("AU-3 재발급하면 새 토큰을 반환한다")
    void refresh_returnsTokens() throws Exception {
        post("/api/v1/auth/token/refresh", "{\"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.refresh\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("eyJhbGciOiJIUzI1NiJ9.access"));
    }

    @Test
    @DisplayName("AU-4 로그아웃하면 성공만 반환한다")
    void logout_returnsSuccess() throws Exception {
        post("/api/v1/auth/logout", "{\"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.refresh\", \"deviceId\": \"%s\"}".formatted(DEVICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$", hasEntry(equalTo("data"), nullValue())));
    }

    private ResultActions login(String provider, String body) throws Exception {
        return post("/api/v1/auth/" + provider + "/login", body);
    }

    private ResultActions post(String uri, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
