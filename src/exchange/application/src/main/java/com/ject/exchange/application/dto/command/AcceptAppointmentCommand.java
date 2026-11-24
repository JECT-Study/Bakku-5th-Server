package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record AcceptAppointmentCommand(
        /**
         * 수락할 약속 ID
         */
        Long appointmentId,

        /**
         * 수락하는 회원 ID. 받는 사람이어야 한다
         */
        Long accepterId

        // 출시 후 검토 (E-06-1, #117): 받는 사람의 약속 전 알림(분). 이번 출시는 30분 고정
        // Integer reminderOffsetMinutes
) implements Command<Long> {
}
