package com.ject.user.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 등록한 경로의 요청을 IP별로 제한한다. 넘으면 COMMON_003(429).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final IpRateLimiter rateLimiter;

    public RateLimitInterceptor(IpRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * 클라이언트 IP는 getRemoteAddr()로 읽는다. 프록시 뒤에서는 Tomcat이 X-Forwarded-For를 오른쪽부터 읽어
     * 내부 프록시가 아닌 첫 주소로 바꿔 둔다 (server.forward-headers-strategy=native). 헤더 왼쪽은 클라이언트가
     * 마음대로 넣을 수 있어서 직접 읽지 않는다.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimiter.tryAcquire(request.getRemoteAddr())) {
            throw new TooManyRequestsException();
        }
        return true;
    }
}
