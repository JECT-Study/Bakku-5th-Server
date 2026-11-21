package com.ject.exchange.domain.domain.appointment.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;

import java.util.List;
import java.util.Objects;

/**
 * 약속에서 교환할 물건 (교환의 첨부 아이템 ID 목록, 1개 이상, 중복 불가)
 */
public record AppointmentItemIds(List<Long> values) {

    public AppointmentItemIds {
        if (values == null || values.isEmpty()
                || values.stream().anyMatch(Objects::isNull)
                || values.stream().distinct().count() != values.size()) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        values = List.copyOf(values);
    }
}
