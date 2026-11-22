package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.command.Command;
import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.command.RejectAppointmentCommand;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.dto.command.WithdrawAppointmentCommand;
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
    private Function<AcceptAppointmentCommand, Long> acceptAppointmentResponder;
    private Function<RejectAppointmentCommand, Long> rejectAppointmentResponder;
    private Function<WithdrawAppointmentCommand, Long> withdrawAppointmentResponder;

    FakeCommandBus(UpdateNewItemHandler updateNewItemHandler) {
        this.updateNewItemHandler = updateNewItemHandler;
    }

    FakeCommandBus() {
        this(null);
    }

    void onProposeAppointment(Function<ProposeAppointmentCommand, Long> responder) {
        this.proposeAppointmentResponder = responder;
    }

    /** A-4 커맨드 응답(약속 id 또는 예외)을 정한다. */
    void onAcceptAppointment(Function<AcceptAppointmentCommand, Long> responder) {
        this.acceptAppointmentResponder = responder;
    }

    /** A-5 커맨드 응답(약속 id 또는 예외)을 정한다. */
    void onRejectAppointment(Function<RejectAppointmentCommand, Long> responder) {
        this.rejectAppointmentResponder = responder;
    }

    /** A-6 커맨드 응답(약속 id 또는 예외)을 정한다. */
    void onWithdrawAppointment(Function<WithdrawAppointmentCommand, Long> responder) {
        this.withdrawAppointmentResponder = responder;
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
        if (command instanceof AcceptAppointmentCommand acceptAppointmentCommand
                && acceptAppointmentResponder != null) {
            return (R) acceptAppointmentResponder.apply(acceptAppointmentCommand);
        }
        if (command instanceof RejectAppointmentCommand rejectAppointmentCommand
                && rejectAppointmentResponder != null) {
            return (R) rejectAppointmentResponder.apply(rejectAppointmentCommand);
        }
        if (command instanceof WithdrawAppointmentCommand withdrawAppointmentCommand
                && withdrawAppointmentResponder != null) {
            return (R) withdrawAppointmentResponder.apply(withdrawAppointmentCommand);
        }
        throw new IllegalStateException("이 테스트가 다루지 않는 커맨드: " + command);
    }

    List<Command<?>> sentCommands() {
        return sentCommands;
    }
}
