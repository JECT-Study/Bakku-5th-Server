package com.ject.notification.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationSwaggerConfig {

    @Bean
    public GroupedOpenApi notificationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("notification")
                .packagesToScan("com.ject.notification.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - notification")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
