package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 이미 가입된 소셜 계정으로 다시 가입하려 할 때 (MEMBER_014). 가입 요청이 중복으로 온 경우다.
 */
public class AlreadySignedUpException extends DomainException {

    public AlreadySignedUpException() {
        super(UserErrorCode.ALREADY_SIGNED_UP);
    }
}
