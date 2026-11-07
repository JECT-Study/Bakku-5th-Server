package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostException extends DomainException {

    public PostException(PostErrorCode errorCode) {
        super(errorCode);
    }
}
