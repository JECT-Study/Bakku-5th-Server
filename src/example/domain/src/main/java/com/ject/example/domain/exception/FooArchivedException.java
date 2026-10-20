package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;
import com.ject.common.exception.ErrorType;

public class FooArchivedException extends DomainException {

    public FooArchivedException() {
        super(ErrorType.CONFLICT, "보관된 Foo는 수정할 수 없습니다.");
    }
}
