package com.ject.exchange.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import lombok.Builder;

public record GetExchangeQuery(
        Long exchangeId,
        Long viewerId
) implements Query<ExchangeResult> {

    @Builder
    public GetExchangeQuery {
    }
}
