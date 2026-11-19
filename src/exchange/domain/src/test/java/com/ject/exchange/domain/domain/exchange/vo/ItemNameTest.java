package com.ject.exchange.domain.domain.exchange.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ItemNameTest {

    @Test
    @DisplayName("제품명이 2자면 그대로 만든다")
    void create_twoCharacters() {
        ItemName name = new ItemName("치이");

        assertEquals("치이", name.value());
    }

    @Test
    @DisplayName("제품명이 40자면 그대로 만든다")
    void create_fortyCharacters() {
        String fortyCharacters = "가".repeat(40);

        ItemName name = new ItemName(fortyCharacters);

        assertEquals(fortyCharacters, name.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "가"})
    @DisplayName("제품명이 null · 빈 문자열 · 공백만 · 1자면 INVALID_OFFERED_ITEMS")
    void create_tooShortOrBlank_throwsInvalidOfferedItems(String value) {
        ExchangeException exception = assertThrows(ExchangeException.class, () -> new ItemName(value));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }

    @Test
    @DisplayName("제품명이 41자면 INVALID_OFFERED_ITEMS")
    void create_fortyOneCharacters_throwsInvalidOfferedItems() {
        String fortyOneCharacters = "가".repeat(41);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> new ItemName(fortyOneCharacters));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }
}
