package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class InvalidImageKeyException extends DomainException {

    public InvalidImageKeyException(int order, String key) {
        super(PostErrorCode.INVALID_IMAGE_KEY, order + "번째 이미지는 본인이 올린 이미지가 아닙니다. key=" + key);
    }
}
