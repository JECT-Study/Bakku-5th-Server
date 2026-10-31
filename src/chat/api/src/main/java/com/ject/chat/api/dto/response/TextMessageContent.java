package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TEXT 내용")
public record TextMessageContent(
        @Schema(description = "본문", example = "내일 3시 홍대입구역 어떠세요?")
        String text,

        @Schema(description = "외부 거래 표현 감지 여부. true면 받은 사람 화면에 경고 + [신고하기]. 2차, 1차는 항상 false", example = "false")
        boolean externalTradeDetected
) implements MessageContent {
}
