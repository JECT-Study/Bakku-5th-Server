package com.ject.example.domain.exception;

import com.ject.common.core.exception.DomainException;

public class InvalidBarContentException extends DomainException {

    public InvalidBarContentException(String message) {
        super(ExampleErrorCode.INVALID_BAR_CONTENT, message);
    }
}
