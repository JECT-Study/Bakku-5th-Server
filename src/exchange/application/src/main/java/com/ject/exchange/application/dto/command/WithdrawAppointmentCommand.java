package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record WithdrawAppointmentCommand(
        /**
         * 철회할 약속 ID
         */
        Long appointmentId,

        /**
         * 철회하는 회원 ID. 제안자여야 한다
         */
        Long withdrawerId
) implements Command<Long> {
}
