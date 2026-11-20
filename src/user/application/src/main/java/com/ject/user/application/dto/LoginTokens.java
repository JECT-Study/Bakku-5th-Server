package com.ject.user.application.dto;

/**
 * 로그인 · 가입 · 재발급 때 주는 토큰과 회원.
 *
 * @param refreshToken 원문. 서버에는 해시만 저장하므로 이 응답 뒤로는 다시 알 수 없다
 */
public record LoginTokens(AccessToken accessToken, String refreshToken, Long memberId, String nickname) {
}
