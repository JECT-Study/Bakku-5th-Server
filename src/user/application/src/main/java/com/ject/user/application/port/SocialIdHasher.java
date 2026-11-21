package com.ject.user.application.port;

/**
 * 소셜 회원번호를 HMAC 해시로 바꾸는 출력 포트. 원문은 저장하지 않고 재가입 비교에만 해시를 쓴다 (결정 A4-1 · C9).
 */
public interface SocialIdHasher {

    String hash(String socialId);
}
