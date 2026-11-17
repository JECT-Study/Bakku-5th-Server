package com.ject.image.application.exception;

import com.ject.common.core.exception.DomainException;
import com.ject.image.domain.exception.RequestTooLargeException;

public final class ImageExceptions {

    private ImageExceptions() {
    }

    public static DomainException requestTooLarge(long maxMegabytes) {
        return new RequestTooLargeException(maxMegabytes);
    }
}
