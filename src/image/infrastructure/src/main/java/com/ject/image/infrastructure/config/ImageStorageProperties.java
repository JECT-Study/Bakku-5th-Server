package com.ject.image.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.image.storage")
public record ImageStorageProperties(
        String endpoint,
        String bucket,
        String region,
        String accessKey,
        String secretKey
) {
}
