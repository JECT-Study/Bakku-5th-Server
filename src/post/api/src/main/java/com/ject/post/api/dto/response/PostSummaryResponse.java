package com.ject.post.api.dto.response;

import com.ject.post.api.dto.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "교환글 목록 항목")
public record PostSummaryResponse(
        @Schema(description = "교환글 ID", example = "10")
        Long id,

        @Schema(description = "대표 사진 URL", example = "https://{cdn}/post/10/8f14e45f.jpg")
        String thumbnailUrl,

        @Schema(description = "제품명", example = "치이카와 하치와레 가챠 키링")
        String title,

        @Schema(description = "구+동. 구가 없으면 시+동", example = "마포구 동교동")
        String regionName,

        @Schema(description = "교환글 상태. AVAILABLE 또는 RESERVED", example = "AVAILABLE")
        PostStatus status,

        @Schema(description = "교환 가능이고 종료 시각 전이면 true", example = "true")
        boolean nowExchange,

        @Schema(description = "지금 교환 종료 시각. 일반 교환이면 null", example = "2026-11-02T15:30:00+09:00")
        OffsetDateTime nowExchangeEndsAt,

        @Schema(description = "원하는 아이템. null이면 '아무거나 상관없어요'", example = "치이카와 가챠 키링 (하치와레, 모몽가 제외)")
        String wantedItem,

        @Schema(description = "내 글 여부", example = "false")
        boolean isMine,

        @Schema(description = "작성 시각", example = "2026-11-02T14:30:00+09:00")
        OffsetDateTime createdAt
) {
}
