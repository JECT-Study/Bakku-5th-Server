package com.ject.chat.domain.domain.message;

/**
 * CARD 메시지 내용 (카드 종류·대상 ID 필수)
 */
public record CardContent(CardType cardType, Long targetId) implements ChatMessageContent {

    public CardContent {
        if (cardType == null || targetId == null) {
            throw new IllegalArgumentException("CARD 메시지는 cardType과 targetId가 필요합니다.");
        }
    }
}
