package com.ject.user.api.auth;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.web.auth.LoginMember;
import com.ject.user.application.exception.InvalidAccessTokenException;
import com.ject.user.application.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationFilterTest {

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        // "valid-{id}"만 통과시키는 인증 서비스
        AuthenticationService authenticationService = new AuthenticationService(null, null) {
            @Override
            public Long authenticate(String accessToken) {
                if (accessToken.startsWith("valid-")) {
                    return Long.valueOf(accessToken.substring("valid-".length()));
                }
                throw InvalidAccessTokenException.expired();
            }
        };
        filter = new AuthenticationFilter(authenticationService, List.of("/api/v1/auth/*/login", "/api/v1/dev/**"),
                JsonMapper.builder().build());
    }

    @Test
    @DisplayName("올바른 토큰이면 회원 id를 요청에 담고 다음으로 넘긴다")
    void authenticated() throws Exception {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/members/me");
        request.addHeader("Authorization", "Bearer valid-7");
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertEquals(7L, request.getAttribute(LoginMember.ATTRIBUTE));
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("토큰이 없거나 Bearer가 아니면 401 AUTH_001을 ApiResponse 형식으로 준다")
    void missingToken() throws Exception {
        for (String header : new String[]{null, "Basic abc", "Bearer "}) {
            // given
            MockHttpServletRequest request = request("GET", "/api/v1/members/me");
            if (header != null) {
                request.addHeader("Authorization", header);
            }
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            // when
            filter.doFilter(request, response, chain);

            // then
            assertEquals(401, response.getStatus());
            String body = response.getContentAsString(StandardCharsets.UTF_8);
            assertTrue(body.contains("\"success\":false"), body);
            assertTrue(body.contains("\"code\":\"AUTH_001\""), body);
            assertNull(chain.getRequest());
        }
    }

    @Test
    @DisplayName("인증 서비스가 실패하면 그 에러 코드로 응답한다")
    void authenticationFailed() throws Exception {
        // given
        MockHttpServletRequest request = request("POST", "/api/v1/exchanges");
        request.addHeader("Authorization", "bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then
        assertEquals(CommonErrorCode.TOKEN_EXPIRED.getStatus(), response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("\"code\":\"AUTH_002\""));
    }

    @Test
    @DisplayName("공개 경로 · /api/ 밖 경로 · OPTIONS 요청은 토큰 없이 지나간다")
    void skipped() throws Exception {
        for (MockHttpServletRequest request : List.of(
                request("POST", "/api/v1/auth/kakao/login"),
                request("POST", "/api/v1/dev/tokens"),
                request("GET", "/swagger-ui/index.html"),
                request("OPTIONS", "/api/v1/members/me"))) {
            // given
            MockFilterChain chain = new MockFilterChain();

            // when
            filter.doFilter(request, new MockHttpServletResponse(), chain);

            // then
            assertNotNull(chain.getRequest(), request.getRequestURI());
            assertNull(request.getAttribute(LoginMember.ATTRIBUTE));
        }
    }

    @Test
    @DisplayName("원문 URI가 공개 경로처럼 보여도 정리된 경로가 보호 경로면 토큰을 요구한다")
    void pathTraversal() throws Exception {
        // given: 컨테이너가 "/api/v1/dev/../members/me"를 "/api/v1/members/me"로 정리한 상태
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dev/../members/me");
        request.setServletPath("/api/v1/members/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);
        return request;
    }
}
