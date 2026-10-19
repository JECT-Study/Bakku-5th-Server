package com.ject.exchange.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExchangeSwaggerConfig {

    @Bean
    public GroupedOpenApi exchangeGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("exchange")
                .packagesToScan("com.ject.exchange.api")
                .addOpenApiCustomizer(openApi -> openApi.getInfo()
                        .title("Bakku-5th-Server API - exchange")
                        .description("Bakku 서비스 API 명세"))
                .build();
    }
}
