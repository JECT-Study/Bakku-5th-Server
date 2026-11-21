package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class InvalidImageKeyException extends DomainException {

    public InvalidImageKeyException(int order, String key) {
        super(PostErrorCode.INVALID_IMAGE_KEY, order + "번째 이미지를 사용할 수 없습니다. key=" + key);
    }
}
