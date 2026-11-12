package com.ject.user.api.auth;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;
import com.ject.common.core.exception.ErrorCode;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.application.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * /api/ 요청마다 Authorization: Bearer 토큰을 확인하고 회원 id를 요청에 담는다 ({@link LoginMember}로 받음).
 * 컨트롤러 밖이라 도메인별 예외 처리기를 거치지 않으므로, 실패 응답은 여기서 ApiResponse 형식으로 직접 쓴다.
 */
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String API_PATH_PREFIX = "/api/";
    private static final String BEARER = "Bearer ";

    private final AuthenticationService authenticationService;
    private final List<String> publicPaths;
    private final JsonMapper jsonMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticationFilter(AuthenticationService authenticationService, List<String> publicPaths,
                                JsonMapper jsonMapper) {
        this.authenticationService = authenticationService;
        this.publicPaths = List.copyOf(publicPaths);
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathOf(request);
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith(API_PATH_PREFIX)
                || publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = bearerTokenOf(request);
        if (token == null) {
            rejected(request, response, CommonErrorCode.TOKEN_MISSING, CommonErrorCode.TOKEN_MISSING.getMessage());
            return;
        }
        Long memberId;
        try {
            memberId = authenticationService.authenticate(token);
        } catch (DomainException e) {
            rejected(request, response, e.getErrorCode(), e.getMessage());
            return;
        } catch (RuntimeException e) {
            log.error("인증 처리 중 예외 {} {}", request.getMethod(), pathOf(request), e);
            writeError(response, CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.getMessage());
            return;
        }
        request.setAttribute(LoginMember.ATTRIBUTE, memberId);
        chain.doFilter(request, response);
    }

    private static String bearerTokenOf(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void rejected(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode,
                          String message) throws IOException {
        // 토큰 값은 로그에 남기지 않는다
        log.warn("[{}] {} {} {}", errorCode.getCode(), request.getMethod(), pathOf(request), message);
        writeError(response, errorCode, message);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.error(errorCode, message).getBody());
    }

    /**
     * 서블릿 컨테이너가 정리한 경로. 원문 URI(getRequestURI)는 "/api/v1/dev/../members/me"처럼
     * 공개 경로로 보이지만 실제로는 다른 API로 가는 요청을 걸러내지 못한다.
     */
    private static String pathOf(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? request.getServletPath() : request.getServletPath() + pathInfo;
    }
}
