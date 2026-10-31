package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;

public class InvalidFooNameException extends DomainException {

    public InvalidFooNameException(String message) {
        super(ExampleErrorCode.INVALID_FOO_NAME, message);
    }
}
