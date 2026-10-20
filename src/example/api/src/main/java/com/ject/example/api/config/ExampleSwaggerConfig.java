package com.ject.example.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExampleSwaggerConfig {

    @Bean
    public GroupedOpenApi exampleGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("example")
                .packagesToScan("com.ject.example.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - example")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
