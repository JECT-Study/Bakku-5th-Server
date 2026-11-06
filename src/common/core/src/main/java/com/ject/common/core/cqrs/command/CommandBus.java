package com.ject.common.core.cqrs.command;

public interface CommandBus {
    <R> R send(Command<R> command);
}
