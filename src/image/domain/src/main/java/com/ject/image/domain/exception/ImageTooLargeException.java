package com.ject.image.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ImageTooLargeException extends DomainException {

    public ImageTooLargeException(int order, long maxMegabytes) {
        super(ImageErrorCode.UNSUPPORTED_IMAGE, order + "번째 파일이 " + maxMegabytes + "MB를 초과했습니다.");
    }
}
