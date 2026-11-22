package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record UpdateReminderCommand(
        /**
         * 알림을 바꿀 약속 ID
         */
        Long appointmentId,

        /**
         * 바꾸는 회원 ID. 약속 참여자여야 한다
         */
        Long memberId,

        /**
         * 내 약속 전 알림(분). null이면 끄기
         */
        Integer reminderOffsetMinutes
) implements Command<Long> {
}
