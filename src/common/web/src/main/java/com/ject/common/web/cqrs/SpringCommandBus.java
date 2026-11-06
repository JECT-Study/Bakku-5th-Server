package com.ject.common.web.cqrs;

import com.ject.common.core.cqrs.command.Command;
import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.command.CommandHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SpringCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers;

    public SpringCommandBus(ObjectProvider<CommandHandler<?, ?>> handlerProvider) {
        Map<Class<?>, CommandHandler<?, ?>> registry = new HashMap<>();
        handlerProvider.stream().forEach(handler -> {
            Class<?> commandType = commandTypeOf(handler);
            if(registry.putIfAbsent(commandType, handler) != null) {
                throw new IllegalStateException("중복 핸들러: " + commandType.getName());
            }
        });
        this.handlers = Map.copyOf(registry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
        if(handler == null) {
            throw new IllegalStateException("핸들러 없음: " + command.getClass().getName());
        }
        return handler.handle(command);
    }

    private static Class<?> commandTypeOf(CommandHandler<?, ?> handler) {
        return ResolvableType.forClass(AopUtils.getTargetClass(handler))
                .as(CommandHandler.class)
                .getGeneric(0)
                .resolve();
    }
}
