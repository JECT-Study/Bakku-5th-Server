package com.ject.exchange.domain.domain.exchange.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;

import java.util.List;
import java.util.Objects;

/**
 * 교환 요청에 첨부할 내 교환글 ID 목록 (1~5개, 중복 불가)
 */
public record OfferedPostIds(List<Long> values) {

    private static final int MAX_SIZE = 5;

    public OfferedPostIds {
        if (values == null || values.isEmpty() || values.size() > MAX_SIZE
                || values.stream().anyMatch(Objects::isNull)
                || values.stream().distinct().count() != values.size()) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        values = List.copyOf(values);
    }
}
