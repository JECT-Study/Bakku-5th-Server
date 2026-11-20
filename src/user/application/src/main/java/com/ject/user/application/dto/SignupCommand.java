package com.ject.user.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 가입 완료 요청 (AU-2). 약관 동의와 프로필을 한 번에 받는다.
 *
 * @param gender Gender enum 이름. 선택 입력이라 null 가능
 */
public record SignupCommand(String signupToken, List<Agreement> agreements, String nickname, LocalDate birthDate,
                            String gender, String deviceId) {

    /**
     * @param termsType TermsType enum 이름
     */
    public record Agreement(String termsType, String version, boolean agreed) {
    }
}
