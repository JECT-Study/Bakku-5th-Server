package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.OfferType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속에서 교환할 물건")
public record AppointmentItemResponse(
        @Schema(description = "첨부 아이템 ID", example = "31")
        Long offeredItemId,

        @Schema(description = "첨부 방식", example = "POST")
        OfferType type,

        @Schema(description = "표시 이름 (POST는 원본 글 현재 제목)", example = "우사기 미니 피규어")
        String name,

        @Schema(description = "대표 사진. 없으면 null", example = "https://{cdn}/post/21/1.jpg")
        String thumbnailUrl
) {
}
