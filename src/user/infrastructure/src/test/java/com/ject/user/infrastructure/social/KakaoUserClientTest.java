package com.ject.user.infrastructure.social;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoUserClientTest {

    private static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";
    private static final String APP_ID = "1234";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    @DisplayName("우리 앱 토큰이면 회원번호를 받고, 생년월일 · 성별은 비어 있다")
    void fetch() {
        // given
        respond("{\"id\":4200,\"expires_in\":7199,\"app_id\":1234}");

        // when
        SocialUser user = client(APP_ID).fetch("kakao-token");

        // then
        assertEquals(SocialProvider.KAKAO, user.provider());
        assertEquals("4200", user.socialId());
        assertNull(user.birthDate());
        assertNull(user.gender());
        server.verify();
    }

    @Test
    @DisplayName("다른 앱에서 발급된 토큰이면 MEMBER_001")
    void otherApp() {
        // given
        respond("{\"id\":4200,\"expires_in\":7199,\"app_id\":9999}");

        // when & then
        assertThrows(InvalidSocialTokenException.class, () -> client(APP_ID).fetch("kakao-token"));
    }

    @Test
    @DisplayName("앱 ID 설정이 없으면 발급 앱을 확인하지 않는다")
    void noAppIdConfigured() {
        // given
        respond("{\"id\":4200,\"expires_in\":7199,\"app_id\":9999}");

        // when
        SocialUser user = client("").fetch("kakao-token");

        // then
        assertEquals("4200", user.socialId());
    }

    @Test
    @DisplayName("만료 · 위조 토큰(401)이나 카카오 서버 오류면 MEMBER_001")
    void invalidTokenOrServerError() {
        // given
        server.expect(requestTo(TOKEN_INFO_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(TOKEN_INFO_URL)).andRespond(withServerError());
        KakaoUserClient client = client(APP_ID);

        // when & then
        assertThrows(InvalidSocialTokenException.class, () -> client.fetch("expired-token"));
        assertThrows(InvalidSocialTokenException.class, () -> client.fetch("kakao-token"));
    }

    private void respond(String body) {
        server.expect(requestTo(TOKEN_INFO_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private KakaoUserClient client(String appId) {
        return new KakaoUserClient(builder.build(), appId);
    }
}
