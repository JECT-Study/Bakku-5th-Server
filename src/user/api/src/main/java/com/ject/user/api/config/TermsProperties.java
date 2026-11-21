package com.ject.user.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param documentBaseUrl 약관 본문 웹뷰 주소 앞부분. 본문 URL은 {documentBaseUrl}/terms/{경로}/{버전}
 */
@ConfigurationProperties(prefix = "app.terms")
public record TermsProperties(String documentBaseUrl) {
}
