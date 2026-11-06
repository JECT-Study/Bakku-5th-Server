package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.AppointmentButton;
import com.ject.exchange.api.dto.enums.ExchangeStatus;
import com.ject.exchange.api.dto.enums.OfferType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "교환")
public record ExchangeResponse(
        @Schema(description = "교환 ID", example = "1")
        Long id,

        @Schema(description = "교환 상태", example = "ACTIVE")
        ExchangeStatus status,

        @Schema(description = "요청한 교환글의 현재 정보 (상단 고정 카드)")
        PostSummaryResponse post,

        @Schema(description = "요청자")
        ParticipantResponse requester,

        @Schema(description = "교환글 게시자")
        ParticipantResponse owner,

        @Schema(description = "첨부 방식", example = "POST")
        OfferType offerType,

        @Schema(description = "첨부 아이템. POST면 1~5개, NEW면 1개")
        List<OfferedItemResponse> offeredItems,

        @Schema(description = "이 교환의 채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "입력창 위 버튼 상태", example = "PROPOSE")
        AppointmentButton appointmentButton,

        @Schema(description = "요청 시각", example = "2026-10-26T14:30:00+09:00")
        OffsetDateTime createdAt
) {
}
