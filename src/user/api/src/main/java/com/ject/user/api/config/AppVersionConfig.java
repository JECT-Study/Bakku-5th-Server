package com.ject.user.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppVersionProperties.class)
public class AppVersionConfig {
}
