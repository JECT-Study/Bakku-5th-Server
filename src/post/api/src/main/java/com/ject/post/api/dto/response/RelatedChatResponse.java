package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연관 채팅")
public record RelatedChatResponse(
        @Schema(description = "이 글로 열린 방이나 이 글이 첨부된 방 중 상대가 나가지 않은 방이 있는지. true면 삭제 팝업에 연결 채팅방 안내 추가", example = "true")
        boolean hasRelatedChat
) {
}
