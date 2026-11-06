package com.ject.example.domain.exception;

import com.ject.common.core.exception.DomainException;

public class FooArchivedException extends DomainException {

    public FooArchivedException() {
        super(ExampleErrorCode.FOO_ARCHIVED);
    }
}
