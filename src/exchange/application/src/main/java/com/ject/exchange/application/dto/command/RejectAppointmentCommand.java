package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record RejectAppointmentCommand(
        /**
         * 거절할 약속 ID
         */
        Long appointmentId,

        /**
         * 거절하는 회원 ID. 받는 사람이어야 한다
         */
        Long rejecterId
) implements Command<Long> {
}
