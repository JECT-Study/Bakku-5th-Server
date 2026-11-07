package com.ject.common.core.cqrs.command;

public interface CommandHandler<C extends  Command<R>, R> {
    R handle(C command);
}
