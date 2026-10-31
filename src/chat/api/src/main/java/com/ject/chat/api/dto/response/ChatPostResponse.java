package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 목록의 교환글 요약")
public record ChatPostResponse(
        @Schema(description = "교환글 ID", example = "10")
        Long id,

        @Schema(description = "교환글 대표 사진. 없으면 null", example = "https://{cdn}/post/10/1.jpg")
        String thumbnailUrl,

        @Schema(description = "교환글 상태", example = "RESERVED")
        PostStatus status
) {
}
