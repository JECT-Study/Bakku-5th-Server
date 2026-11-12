package com.ject.common.web.auth;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;

/**
 * 로그인 회원이 필요한데 요청에 회원 id가 없을 때 (AUTH_001).
 */
public class LoginRequiredException extends DomainException {

    public LoginRequiredException() {
        super(CommonErrorCode.TOKEN_MISSING);
    }
}
