package com.ject.exchange.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ExchangeException extends DomainException {

    public ExchangeException(ExchangeErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 응답 메시지에 구체적인 대상을 담아야 할 때 (예: 몇 번째 사진인지)
     */
    public ExchangeException(ExchangeErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
