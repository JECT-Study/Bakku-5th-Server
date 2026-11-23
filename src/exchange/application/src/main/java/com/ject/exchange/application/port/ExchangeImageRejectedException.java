package com.ject.exchange.application.port;

import lombok.Getter;

/**
 * image BC가 사진 key를 받아들이지 않았다. 몇 번째 사진인지 응답에 담는다
 */
@Getter
public class ExchangeImageRejectedException extends RuntimeException {

    /** 1부터 */
    private final int order;
    private final String key;

    public ExchangeImageRejectedException(int order, String key) {
        super(order + "번째 이미지를 사용할 수 없습니다. key=" + key);
        this.order = order;
        this.key = key;
    }
}
