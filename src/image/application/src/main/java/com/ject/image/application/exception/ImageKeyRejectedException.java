package com.ject.image.application.exception;

import lombok.Getter;

@Getter
public class ImageKeyRejectedException extends RuntimeException {

    private final int order;
    private final String key;

    public ImageKeyRejectedException(int order, String key) {
        super(order + "번째 이미지 key를 사용할 수 없습니다. key=" + key);
        this.order = order;
        this.key = key;
    }
}
