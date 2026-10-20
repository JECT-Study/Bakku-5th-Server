package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;
import com.ject.common.exception.ErrorType;

public class BarLimitExceededException extends DomainException {

    public BarLimitExceededException(int maxBarCount) {
        super(ErrorType.CONFLICT, "Bar는 최대 " + maxBarCount + "개까지 추가할 수 있습니다.");
    }
}
