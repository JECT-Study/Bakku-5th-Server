package com.ject.exchange.domain.exception;

import com.ject.common.exception.DomainException;

public class ExchangeException extends DomainException {

    public ExchangeException(ExchangeErrorCode errorCode) {
        super(errorCode);
    }
}
