package com.ject.example.domain.exception;

import com.ject.common.exception.DomainException;

public class BarLimitExceededException extends DomainException {

    public BarLimitExceededException(int maxBarCount) {
        super(ExampleErrorCode.BAR_LIMIT_EXCEEDED, "Bar는 최대 " + maxBarCount + "개까지 추가할 수 있습니다.");
    }
}
