package com.ject.user.application.dto;

import java.time.Instant;

/**
 * 가입 완료(AU-2)에 보낼 토큰. 유효 7일이라 가입 도중 앱을 꺼도 이어서 가입할 수 있다 (결정 A2-1).
 */
public record SignupToken(String value, Instant expiresAt) {
}
