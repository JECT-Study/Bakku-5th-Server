package com.ject.user.api.dto.enums;

/**
 * 가입에 쓴 소셜 계정. 탈퇴 때 FE가 어느 SDK로 연결을 끊을지 고른다 (결정 C17). APPLE은 iOS만.
 */
public enum SocialProvider {
    KAKAO,
    NAVER,
    APPLE
}
