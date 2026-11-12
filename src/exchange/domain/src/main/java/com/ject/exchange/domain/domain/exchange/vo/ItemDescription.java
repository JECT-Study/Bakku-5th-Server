package com.ject.exchange.domain.domain.exchange.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;

/**
 * 새로 작성한 아이템의 자세한 설명 (선택, 1000자 이하). 값이 없으면 value는 null
 */
public record ItemDescription(String value) {

    private static final int MAX_LENGTH = 1000;

    public ItemDescription {
        if (value != null && value.length() > MAX_LENGTH) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
    }
}
