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
        Long accepterId,

        /**
         * 받는 사람의 약속 전 알림(분). null이면 끄기. 지금 바로 약속이면 무시
         */
        Integer reminderOffsetMinutes
) implements Command<Long> {
}
