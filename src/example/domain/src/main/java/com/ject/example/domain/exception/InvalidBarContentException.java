package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;
import com.ject.common.exception.ErrorType;

public class InvalidBarContentException extends DomainException {

    public InvalidBarContentException(String message) {
        super(ErrorType.INVALID_INPUT, message);
    }
}
