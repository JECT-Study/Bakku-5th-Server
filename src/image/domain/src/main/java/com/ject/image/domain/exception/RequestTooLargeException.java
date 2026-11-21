package com.ject.image.domain.exception;

import com.ject.common.core.exception.DomainException;

public class RequestTooLargeException extends DomainException {

    public RequestTooLargeException(long maxMegabytes) {
        super(ImageErrorCode.UNSUPPORTED_IMAGE, "한 번에 올릴 수 있는 용량(" + maxMegabytes + "MB)을 초과했습니다.");
    }
}
