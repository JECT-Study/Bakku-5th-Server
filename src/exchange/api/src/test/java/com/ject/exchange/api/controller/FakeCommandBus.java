package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.command.Command;
import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.service.command.UpdateNewItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 테스트용 가짜 커맨드 버스. 받은 커맨드를 기록하고, E-3 커맨드는 실제 Handler로 넘긴다.
 */
class FakeCommandBus implements CommandBus {

    private final UpdateNewItemHandler updateNewItemHandler;
    private final List<Command<?>> sentCommands = new ArrayList<>();

    FakeCommandBus(UpdateNewItemHandler updateNewItemHandler) {
        this.updateNewItemHandler = updateNewItemHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        sentCommands.add(command);
        if (command instanceof UpdateNewItemCommand updateNewItemCommand) {
            return (R) updateNewItemHandler.handle(updateNewItemCommand);
        }
        throw new IllegalStateException("이 테스트가 다루지 않는 커맨드: " + command);
    }

    List<Command<?>> sentCommands() {
        return sentCommands;
    }
}
