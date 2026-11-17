package com.ject.image.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ImageNotFoundException extends DomainException {

    public ImageNotFoundException(String key) {
        super(ImageErrorCode.IMAGE_NOT_FOUND, "이미지를 찾을 수 없습니다. key=" + key);
    }
}
