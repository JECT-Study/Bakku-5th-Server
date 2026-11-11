package com.ject.common.web.idempotency;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;

public class IdempotencyException extends DomainException {
    public IdempotencyException() {
        super(CommonErrorCode.IDEMPOTENCY_IN_PROGRESS);
    }
}
