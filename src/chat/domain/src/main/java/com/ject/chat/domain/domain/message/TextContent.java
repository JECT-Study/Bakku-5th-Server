package com.ject.chat.domain.domain.message;

/**
 * TEXT 메시지 내용 (공백 아닌 본문 1~1,000자)
 */
public record TextContent(String text, boolean externalTradeDetected) implements ChatMessageContent {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 1000;

    public TextContent {
        if (text == null || text.isBlank() || text.length() < MIN_LENGTH || text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("TEXT 메시지는 공백이 아닌 1~1,000자 본문이 필요합니다.");
        }
    }
}
