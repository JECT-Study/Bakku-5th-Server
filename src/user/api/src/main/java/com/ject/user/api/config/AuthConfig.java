package com.ject.user.api.config;

import com.ject.common.web.auth.LoginMember;
import com.ject.user.api.auth.AuthProperties;
import com.ject.user.api.auth.AuthenticationFilter;
import com.ject.user.application.service.AuthenticationService;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    static {
        // @LoginMember 파라미터는 토큰에서 채우므로 Swagger 요청 파라미터로 보이지 않게 한다
        SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginMember.class);
    }

    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilter(AuthenticationService authenticationService,
                                                                             AuthProperties authProperties,
                                                                             JsonMapper jsonMapper) {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>(
                new AuthenticationFilter(authenticationService, authProperties.publicPaths(), jsonMapper));
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
