package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.CardType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CARD 내용. 카드 상태는 targetId로 Exchange API를 다시 조회한다")
public record CardMessageContent(
        @Schema(description = "카드 종류", example = "APPOINTMENT_PROPOSAL")
        CardType cardType,

        @Schema(description = "카드 대상 ID (CardType에 따라 exchangeId · appointmentId · changeId · cancellationId)", example = "3")
        Long targetId
) implements MessageContent {
}
