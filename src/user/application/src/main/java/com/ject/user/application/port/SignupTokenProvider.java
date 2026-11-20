package com.ject.user.application.port;

import com.ject.user.application.dto.SignupIdentity;
import com.ject.user.application.dto.SignupToken;

/**
 * 가입 토큰 발급 · 검증 출력 포트. 서명 방식(JWT)은 infrastructure가 담당한다.
 */
public interface SignupTokenProvider {

    SignupToken issue(SignupIdentity identity);

    /**
     * @throws com.ject.user.application.exception.InvalidSignupTokenException 만료 · 위조 (MEMBER_010)
     */
    SignupIdentity verify(String token);
}
