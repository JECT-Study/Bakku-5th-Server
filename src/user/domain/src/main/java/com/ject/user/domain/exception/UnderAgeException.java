package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class UnderAgeException extends DomainException {

    public UnderAgeException() {
        super(UserErrorCode.UNDER_AGE);
    }
}
