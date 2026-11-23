package com.ject.notification.api.exception;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;

public class InvalidCursorException extends DomainException {

    public InvalidCursorException(String cursor) {
        super(CommonErrorCode.INVALID_INPUT, "cursor 형식이 올바르지 않습니다. cursor=" + cursor);
    }
}
