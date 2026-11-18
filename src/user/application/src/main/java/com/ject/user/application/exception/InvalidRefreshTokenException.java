package com.ject.user.application.exception;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;
import com.ject.common.core.exception.ErrorCode;

/**
 * 리프레시 토큰으로 재발급할 수 없음. 무효(AUTH_004)면 FE가 로그인 화면으로 가고,
 * 이미 재발급됨(AUTH_005)이면 기기에 저장된 새 토큰으로 다시 시도한다 (결정 C3).
 */
public class InvalidRefreshTokenException extends DomainException {

    private InvalidRefreshTokenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static InvalidRefreshTokenException invalid() {
        return new InvalidRefreshTokenException(CommonErrorCode.REFRESH_TOKEN_INVALID);
    }

    public static InvalidRefreshTokenException alreadyReissued() {
        return new InvalidRefreshTokenException(CommonErrorCode.TOKEN_ALREADY_REISSUED);
    }
}
