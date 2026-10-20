package com.ject.chat.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatSwaggerConfig {

    @Bean
    public GroupedOpenApi chatGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("chat")
                .packagesToScan("com.ject.chat.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - chat")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
