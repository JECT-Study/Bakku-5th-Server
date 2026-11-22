package com.ject.exchange.api.dto.request;

import com.ject.exchange.api.dto.enums.ConfirmationAnswer;
import com.ject.exchange.api.dto.enums.NotMetReason;
import com.ject.exchange.api.dto.validation.ConfirmationReasonMatchesAnswer;
import com.ject.exchange.application.dto.command.RespondConfirmationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "교환 확인 응답")
@ConfirmationReasonMatchesAnswer
public record ConfirmationRequest(
        @Schema(description = "교환 확인 응답", example = "DONE")
        @NotNull
        ConfirmationAnswer answer,

        @Schema(description = "NOT_MET일 때 필수. DONE이면 null")
        NotMetReason notMetReason,

        @Schema(description = "notMetReason이 OTHER일 때 직접 입력 (0~100자). OTHER가 아니면 null")
        @Size(max = 100)
        String notMetDetail
) {

    public RespondConfirmationCommand toCommand(Long appointmentId, Long memberId) {
        return new RespondConfirmationCommand(
                appointmentId,
                memberId,
                com.ject.exchange.domain.domain.appointment.ConfirmationAnswer.valueOf(answer.name()),
                notMetReason == null ? null
                        : com.ject.exchange.domain.domain.appointment.NotMetReason.valueOf(notMetReason.name()),
                notMetDetail);
    }
}
