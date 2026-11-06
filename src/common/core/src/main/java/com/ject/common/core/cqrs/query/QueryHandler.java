package com.ject.common.core.cqrs.query;

public interface QueryHandler<C extends Query<R>, R> {
    R handle(C command);
}
