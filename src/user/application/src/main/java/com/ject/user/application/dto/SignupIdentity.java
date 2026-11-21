package com.ject.user.application.dto;

import com.ject.user.domain.domain.SocialProvider;

/**
 * 가입 토큰에 담긴 소셜 계정. 회원번호 원문 대신 해시만 담는다 (결정 C9).
 */
public record SignupIdentity(SocialProvider provider, String socialIdHash) {
}
