package com.ject.common.web.cqrs;

import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.core.cqrs.query.QueryHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SpringQueryBus implements QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers;

    public SpringQueryBus(ObjectProvider<QueryHandler<?, ?>> handlerProvider) {
        Map<Class<?>, QueryHandler<?, ?>> registry = new HashMap<>();
        handlerProvider.stream().forEach(handler -> {
            Class<?> QueryType = QueryTypeOf(handler);
            if(registry.putIfAbsent(QueryType, handler) != null) {
                throw new IllegalStateException("중복 핸들러: " + QueryType.getName());
            }
        });
        this.handlers = Map.copyOf(registry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> Query) {
        QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) handlers.get(Query.getClass());
        if(handler == null) {
            throw new IllegalStateException("핸들러 없음: " + Query.getClass().getName());
        }
        return handler.handle(Query);
    }

    private static Class<?> QueryTypeOf(QueryHandler<?, ?> handler) {
        return ResolvableType.forClass(AopUtils.getTargetClass(handler))
                .as(QueryHandler.class)
                .getGeneric(0)
                .resolve();
    }
}
