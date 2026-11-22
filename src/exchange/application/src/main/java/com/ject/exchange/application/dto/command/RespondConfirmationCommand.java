package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;

public record RespondConfirmationCommand(
        /**
         * 교환 확인에 응답할 약속 ID
         */
        Long appointmentId,

        /**
         * 응답하는 회원 ID. 약속 참여자여야 한다
         */
        Long memberId,

        /**
         * 교환했어요(DONE) 또는 못 만났어요(NOT_MET)
         */
        ConfirmationAnswer answer,

        /**
         * 못 만난 이유. DONE이면 null
         */
        NotMetReason notMetReason,

        /**
         * 직접 입력 (OTHER일 때만, 0~100자)
         */
        String notMetDetail
) implements Command<Long> {
}
