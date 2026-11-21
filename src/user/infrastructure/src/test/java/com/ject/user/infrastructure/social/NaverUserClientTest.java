package com.ject.user.infrastructure.social;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverUserClientTest {

    private static final String PROFILE_URL = "https://openapi.naver.com/v1/nid/me";

    private MockRestServiceServer server;
    private NaverUserClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NaverUserClient(builder.build());
    }

    @Test
    @DisplayName("회원번호와 함께 출생연도 + 생일로 생년월일, F · M으로 성별을 채운다")
    void fetch() {
        // given
        respond("{\"resultcode\":\"00\",\"message\":\"success\",\"response\":"
                + "{\"id\":\"naver-id\",\"gender\":\"F\",\"birthday\":\"03-15\",\"birthyear\":\"2001\"}}");

        // when
        SocialUser user = client.fetch("naver-token");

        // then
        assertEquals(SocialProvider.NAVER, user.provider());
        assertEquals("naver-id", user.socialId());
        assertEquals(LocalDate.of(2001, 3, 15), user.birthDate());
        assertEquals(Gender.FEMALE, user.gender());
        server.verify();
    }

    @Test
    @DisplayName("출생연도나 생일에 동의하지 않았거나 성별이 U면 그 값은 비운다")
    void missingProfileValues() {
        // given
        respond("{\"resultcode\":\"00\",\"message\":\"success\",\"response\":"
                + "{\"id\":\"naver-id\",\"gender\":\"U\",\"birthday\":\"03-15\"}}");

        // when
        SocialUser user = client.fetch("naver-token");

        // then
        assertNull(user.birthDate());
        assertNull(user.gender());
    }

    @Test
    @DisplayName("만료 · 위조 토큰(401)이면 MEMBER_001")
    void invalidToken() {
        // given
        server.expect(requestTo(PROFILE_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when & then
        assertThrows(InvalidSocialTokenException.class, () -> client.fetch("naver-token"));
    }

    @Test
    @DisplayName("결과 코드가 성공(00)이 아니면 MEMBER_001")
    void failedResultCode() {
        // given
        respond("{\"resultcode\":\"024\",\"message\":\"Authentication failed\"}");

        // when & then
        assertThrows(InvalidSocialTokenException.class, () -> client.fetch("naver-token"));
    }

    private void respond(String body) {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer naver-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
