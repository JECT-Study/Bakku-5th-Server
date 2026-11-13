package com.ject.image.domain.exception;

import com.ject.common.core.exception.DomainException;

public class UnsupportedImageTypeException extends DomainException {

    public UnsupportedImageTypeException(int order, String contentType) {
        super(ImageErrorCode.UNSUPPORTED_IMAGE,
                order + "번째 파일은 지원하지 않는 형식입니다 (" + contentType + "). JPEG, PNG, WEBP만 올릴 수 있습니다.");
    }
}
