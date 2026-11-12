package com.ject.user.application.dto;

/**
 * dev 테스트 토큰 발급 결과.
 */
public record DevToken(Long memberId, String nickname, AccessToken accessToken) {
}
