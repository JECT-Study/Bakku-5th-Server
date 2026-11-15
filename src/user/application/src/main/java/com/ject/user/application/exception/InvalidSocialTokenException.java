package com.ject.user.application.exception;

import com.ject.common.core.exception.DomainException;
import com.ject.user.domain.exception.UserErrorCode;

/**
 * 소셜 토큰 검증 실패 (MEMBER_001). 만료 · 위조 · 다른 앱 토큰 · 소셜 서버 오류를 구분하지 않는다.
 */
public class InvalidSocialTokenException extends DomainException {

    public InvalidSocialTokenException(String message) {
        super(UserErrorCode.SOCIAL_TOKEN_INVALID, message);
    }
}
