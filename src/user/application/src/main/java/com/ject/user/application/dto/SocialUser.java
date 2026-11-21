package com.ject.user.application.dto;

import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.SocialProvider;

import java.time.LocalDate;

/**
 * 소셜 사용자 정보 API로 확인한 사용자.
 *
 * @param socialId  소셜 회원번호 원문. 저장하지 않고 해시만 남긴다 (결정 A4-1)
 * @param birthDate 가입 화면 자동 채우기용. 네이버만 채우고, 카카오 · Apple은 null (결정 A18)
 * @param gender    가입 화면 자동 채우기용. 네이버만 채우고, 카카오 · Apple은 null (결정 A18)
 */
public record SocialUser(SocialProvider provider, String socialId, LocalDate birthDate, Gender gender) {
}
