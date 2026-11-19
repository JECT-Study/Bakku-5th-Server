package com.ject.exchange.domain.domain.exchange.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ItemDescriptionTest {

    @Test
    @DisplayName("설명이 null이면 비어 있는 설명으로 만든다")
    void create_null() {
        ItemDescription description = new ItemDescription(null);

        assertNull(description.value());
    }

    @Test
    @DisplayName("설명이 1,000자면 그대로 만든다")
    void create_thousandCharacters() {
        String thousandCharacters = "가".repeat(1000);

        ItemDescription description = new ItemDescription(thousandCharacters);

        assertEquals(1000, description.value().length());
    }

    @Test
    @DisplayName("설명이 1,001자면 INVALID_OFFERED_ITEMS")
    void create_thousandOneCharacters_throwsInvalidOfferedItems() {
        String thousandOneCharacters = "가".repeat(1001);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> new ItemDescription(thousandOneCharacters));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }
}
