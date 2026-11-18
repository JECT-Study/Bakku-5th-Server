package com.ject.user.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 등록한 경로의 요청을 IP별로 제한한다. 넘으면 COMMON_003(429).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final IpRateLimiter rateLimiter;

    public RateLimitInterceptor(IpRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimiter.tryAcquire(clientIp(request))) {
            throw new TooManyRequestsException();
        }
        return true;
    }

    /**
     * 로드 밸런서 뒤에서는 X-Forwarded-For의 첫 값이 클라이언트 IP다 (결정 C7).
     */
    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
