package com.ject.user.application.exception;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;
import com.ject.common.core.exception.ErrorCode;

/**
 * 액세스 토큰 검증 실패. 만료(AUTH_002)면 FE가 AU-3으로 재발급하고, 위조(AUTH_003)면 다시 로그인한다.
 */
public class InvalidAccessTokenException extends DomainException {

    private InvalidAccessTokenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static InvalidAccessTokenException expired() {
        return new InvalidAccessTokenException(CommonErrorCode.TOKEN_EXPIRED);
    }

    public static InvalidAccessTokenException invalid() {
        return new InvalidAccessTokenException(CommonErrorCode.TOKEN_INVALID);
    }
}
