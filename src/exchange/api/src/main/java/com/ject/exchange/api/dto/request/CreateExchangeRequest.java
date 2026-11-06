package com.ject.exchange.api.dto.request;

import com.ject.exchange.api.dto.enums.OfferType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "교환 요청")
public record CreateExchangeRequest(
        @Schema(description = "요청할 교환글 ID", example = "10")
        @NotNull
        Long postId,

        @Schema(description = "첨부 방식. POST 내 교환글 / NEW 새로 작성", example = "POST")
        @NotNull
        OfferType offerType,

        @Schema(description = "POST일 때 필수. 내 교환글 ID 1~5개, 중복 불가. NEW면 null", example = "[21, 22]")
        @Size(min = 1, max = 5)
        List<@NotNull Long> offeredPostIds,

        @Schema(description = "NEW일 때 필수. POST면 null")
        @Valid
        NewItemRequest newItem
) {
}
