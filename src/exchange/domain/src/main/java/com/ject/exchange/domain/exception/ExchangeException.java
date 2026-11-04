package com.ject.exchange.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ExchangeException extends DomainException {

    public ExchangeException(ExchangeErrorCode errorCode) {
        super(errorCode);
    }
}
