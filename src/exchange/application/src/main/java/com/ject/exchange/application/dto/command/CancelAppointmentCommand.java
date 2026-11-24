package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record CancelAppointmentCommand(
        /**
         * 취소할 약속 ID
         */
        Long appointmentId,

        /**
         * 취소하는 회원 ID. 약속 참여자(제안자 또는 받는 사람)여야 한다
         */
        Long cancelerId
) implements Command<Long> {
}
