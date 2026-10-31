package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;

public class FooArchivedException extends DomainException {

    public FooArchivedException() {
        super(ExampleErrorCode.FOO_ARCHIVED);
    }
}
