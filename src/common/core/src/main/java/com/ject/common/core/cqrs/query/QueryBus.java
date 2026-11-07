package com.ject.common.core.cqrs.query;

public interface QueryBus {
    <R> R send(Query<R> query);
}
