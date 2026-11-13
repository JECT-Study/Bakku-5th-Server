package com.ject.user.application.dto;

import java.time.Instant;

/**
 * 발급한 액세스 토큰과 만료 시각 (발급 후 1시간, 결정 A1).
 */
public record AccessToken(String value, Instant expiresAt) {
}
