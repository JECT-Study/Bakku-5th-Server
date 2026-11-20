package com.ject.image.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageSwaggerConfig {

    @Bean
    public GroupedOpenApi imageGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("image")
                .packagesToScan("com.ject.image.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - image")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
