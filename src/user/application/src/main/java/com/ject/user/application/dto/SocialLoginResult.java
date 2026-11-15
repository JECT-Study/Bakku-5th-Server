package com.ject.user.application.dto;

import java.time.LocalDate;

/**
 * 소셜 로그인 결과. 기존 회원이면 tokens, 처음이면 signupToken과 가입 화면 자동 채우기 값을 준다 (AU-1).
 *
 * @param prefillGender Gender enum 이름. 네이버만 채우고 그 밖에는 null
 */
public record SocialLoginResult(LoginTokens tokens, SignupToken signupToken, LocalDate prefillBirthDate,
                                String prefillGender) {

    public static SocialLoginResult login(LoginTokens tokens) {
        return new SocialLoginResult(tokens, null, null, null);
    }

    public static SocialLoginResult signupRequired(SignupToken signupToken, SocialUser user) {
        String gender = user.gender() == null ? null : user.gender().name();
        return new SocialLoginResult(null, signupToken, user.birthDate(), gender);
    }

    public boolean signupRequired() {
        return tokens == null;
    }
}
