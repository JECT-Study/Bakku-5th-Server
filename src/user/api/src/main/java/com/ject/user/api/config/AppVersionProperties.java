package com.ject.user.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 플랫폼별 최소 지원 버전 · 스토어 주소 (AP-1). 배포 없이 환경변수로 바꾼다 (결정 C16).
 */
@ConfigurationProperties(prefix = "app.version")
public record AppVersionProperties(Platform ios, Platform android) {

    /**
     * @param minimumVersion 이보다 낮은 앱은 강제 업데이트 (A-09-1)
     */
    public record Platform(String minimumVersion, String storeUrl) {
    }
}
