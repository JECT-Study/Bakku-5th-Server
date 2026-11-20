package com.ject.user.api.ratelimit;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;

/**
 * 요청 제한을 넘었을 때 (COMMON_003).
 */
public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException() {
        super(CommonErrorCode.TOO_MANY_REQUESTS);
    }
}
