package com.ject.image.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ImageAccessDeniedException extends DomainException {

    public ImageAccessDeniedException(String key) {
        super(ImageErrorCode.IMAGE_ACCESS_DENIED, "이미지를 볼 권한이 없습니다. key=" + key);
    }
}
