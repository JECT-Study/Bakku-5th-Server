package com.ject.exchange.domain.domain.exchange;

import lombok.Getter;

//Todo: Image 모듈 구현 후 구현 예정
@Getter
public class ExchangeImage {

    private final Long id;
    private final String imageKey;
    private final int sortOrder;

    private ExchangeImage(Long id, String imageKey, int sortOrder) {
        this.id = id;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}
