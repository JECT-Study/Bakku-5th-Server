package com.ject.user.application.exception;

import com.ject.common.core.exception.DomainException;
import com.ject.user.domain.exception.UserErrorCode;

/**
 * 가입 토큰 만료 · 위조 (MEMBER_010). FE는 기기에 보관한 가입 값을 지우고 로그인부터 다시 한다.
 */
public class InvalidSignupTokenException extends DomainException {

    public InvalidSignupTokenException() {
        super(UserErrorCode.SIGNUP_TOKEN_INVALID);
    }
}
