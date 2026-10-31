package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.ItemAvailability;
import com.ject.exchange.api.dto.enums.ItemCondition;
import com.ject.exchange.api.dto.enums.OfferType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "교환 요청에 첨부된 아이템")
public record OfferedItemResponse(
        @Schema(description = "첨부 아이템 ID. 약속의 offeredItemIds에 사용", example = "31")
        Long id,

        @Schema(description = "POST 내 교환글 / NEW 새로 작성", example = "POST")
        OfferType type,

        @Schema(description = "교환 가능 상태", example = "AVAILABLE")
        ItemAvailability availability,

        @Schema(description = "내가 새로 작성한 아이템이고 확정 약속이 없으면 true", example = "false")
        boolean editable,

        @Schema(description = "POST일 때 원본 교환글의 현재 정보. NEW면 null")
        PostSummaryResponse post,

        @Schema(description = "NEW일 때 제품명. POST면 null", example = "치이카와 아크릴 스탠드")
        String name,

        @Schema(description = "NEW일 때 상품 상태. POST면 null", example = "UNOPENED")
        ItemCondition condition,

        @Schema(description = "NEW일 때 자세한 설명. POST면 null", example = "가챠에서 두 개 나와서 하나 교환해요")
        String description,

        @Schema(description = "NEW일 때 사진 (첫 장이 대표). POST면 null")
        List<String> imageUrls
) {
}
