package com.ject.exchange.domain.domain.exchange.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import lombok.Builder;

/**
 * 새로 작성한 아이템의 제품명 (공백 불가, 2~40자)
 */
public record ItemName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 40;

    @Builder
    public ItemName {
        if (value == null || value.isBlank()
                || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
    }
}
