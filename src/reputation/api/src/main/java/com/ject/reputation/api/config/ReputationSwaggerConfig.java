package com.ject.reputation.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReputationSwaggerConfig {

    @Bean
    public GroupedOpenApi reputationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("reputation")
                .packagesToScan("com.ject.reputation.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - reputation")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
