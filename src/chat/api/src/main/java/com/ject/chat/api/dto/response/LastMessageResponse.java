package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "채팅 목록의 마지막 메시지")
public record LastMessageResponse(
        @Schema(description = "메시지 ID", example = "120")
        Long messageId,

        @Schema(description = "한 줄 미리보기. 텍스트는 본문, 사진은 \"사진\", 장소는 \"장소\", 시스템 · 카드는 서버가 만든 요약 문구", example = "내일 3시 괜찮으세요?")
        String preview,

        @Schema(description = "보낸 시각", example = "2026-10-12T14:30:00+09:00")
        OffsetDateTime sentAt
) {
}
