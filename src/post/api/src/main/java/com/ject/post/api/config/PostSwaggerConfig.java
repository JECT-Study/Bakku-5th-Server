package com.ject.post.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostSwaggerConfig {

    @Bean
    public GroupedOpenApi postGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("post")
                .packagesToScan("com.ject.post.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - post")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
