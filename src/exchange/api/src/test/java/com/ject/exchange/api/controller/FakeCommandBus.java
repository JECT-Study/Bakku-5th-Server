package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.command.Command;
import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.service.command.UpdateNewItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 테스트용 가짜 커맨드 버스. 받은 커맨드를 기록하고, E-3 커맨드는 실제 Handler로 넘긴다.
 * A-1 커맨드는 테스트가 정한 응답(약속 id 또는 예외)을 돌려준다.
 */
class FakeCommandBus implements CommandBus {

    private final UpdateNewItemHandler updateNewItemHandler;
    private final List<Command<?>> sentCommands = new ArrayList<>();
    private Function<ProposeAppointmentCommand, Long> proposeAppointmentResponder;

    FakeCommandBus(UpdateNewItemHandler updateNewItemHandler) {
        this.updateNewItemHandler = updateNewItemHandler;
    }

    FakeCommandBus() {
        this(null);
    }

    void onProposeAppointment(Function<ProposeAppointmentCommand, Long> responder) {
        this.proposeAppointmentResponder = responder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        sentCommands.add(command);
        if (command instanceof UpdateNewItemCommand updateNewItemCommand) {
            return (R) updateNewItemHandler.handle(updateNewItemCommand);
        }
        if (command instanceof ProposeAppointmentCommand proposeAppointmentCommand
                && proposeAppointmentResponder != null) {
            return (R) proposeAppointmentResponder.apply(proposeAppointmentCommand);
        }
        throw new IllegalStateException("이 테스트가 다루지 않는 커맨드: " + command);
    }

    List<Command<?>> sentCommands() {
        return sentCommands;
    }
}
