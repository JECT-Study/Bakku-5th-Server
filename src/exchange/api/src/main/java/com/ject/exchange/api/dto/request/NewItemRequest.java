package com.ject.exchange.api.dto.request;

import com.ject.exchange.api.dto.enums.ItemCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "새로 작성한 아이템 (E-1의 newItem, E-3 요청 본문)")
public record NewItemRequest(
        @Schema(description = "업로드한 이미지 key 1~5장, 첫 장이 대표", example = "[\"exchange/tmp/a.jpg\", \"exchange/tmp/b.jpg\"]")
        @NotNull @Size(min = 1, max = 5)
        List<@NotBlank String> imageKeys,

        @Schema(description = "제품명 (2~40자)", example = "치이카와 아크릴 스탠드")
        @NotBlank @Size(min = 2, max = 40)
        String name,

        @Schema(description = "상품 상태. null이면 지움", example = "UNOPENED")
        ItemCondition condition,

        @Schema(description = "자세한 설명 (0~1,000자)", example = "가챠에서 두 개 나와서 하나 교환해요")
        @Size(max = 1000)
        String description
) {
}
