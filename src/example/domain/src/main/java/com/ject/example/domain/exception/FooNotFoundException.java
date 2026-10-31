package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;

public class FooNotFoundException extends DomainException {

    public FooNotFoundException(Long id) {
        super(ExampleErrorCode.FOO_NOT_FOUND, "Foo를 찾을 수 없습니다. id=" + id);
    }
}
