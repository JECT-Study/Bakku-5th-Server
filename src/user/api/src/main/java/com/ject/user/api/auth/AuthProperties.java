package com.ject.user.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param publicPaths 로그인 없이 부를 수 있는 /api/ 경로 (Ant 패턴). 다른 도메인도 application.yml에 추가한다
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(List<String> publicPaths) {

    public AuthProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
