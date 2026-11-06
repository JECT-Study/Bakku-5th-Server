package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교환글의 현재 정보")
public record PostSummaryResponse(
        @Schema(description = "교환글 ID", example = "10")
        Long id,

        @Schema(description = "교환글 제목. 삭제된 글이면 null", example = "치이카와 우사기 가챠 피규어")
        String title,

        @Schema(description = "대표 사진. 없으면 null", example = "https://{cdn}/post/10/1.jpg")
        String thumbnailUrl,

        @Schema(description = "교환글 상태", example = "AVAILABLE")
        PostStatus status
) {
}
