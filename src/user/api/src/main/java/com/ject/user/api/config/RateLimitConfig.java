package com.ject.user.api.config;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.api.ratelimit.IpRateLimiter;
import com.ject.user.api.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    // 닉네임 목록 긁기 방지: IP당 분당 30회 (결정 A7)
    private static final int NICKNAME_CHECK_LIMIT = 30;
    private static final Duration NICKNAME_CHECK_WINDOW = Duration.ofMinutes(1);

    private final ServiceTime serviceTime;

    public RateLimitConfig(ServiceTime serviceTime) {
        this.serviceTime = serviceTime;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(
                        new IpRateLimiter(NICKNAME_CHECK_LIMIT, NICKNAME_CHECK_WINDOW, serviceTime)))
                .addPathPatterns("/api/v1/members/nickname-availability");
    }
}
