package com.ject.exchange.api.dto.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class TenMinuteStepValidatorTest {

    private final TenMinuteStepValidator validator = new TenMinuteStepValidator();

    @Test
    @DisplayName("null은 알림 끔이라 통과한다")
    void isValid_null_isTrue() {
        assertTrue(validator.isValid(null, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 30, 60, 1440})
    @DisplayName("10의 배수면 통과한다")
    void isValid_multipleOfTen_isTrue(int minutes) {
        assertTrue(validator.isValid(minutes, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 15, 1, 1441, 1435})
    @DisplayName("10의 배수가 아니면 실패한다")
    void isValid_notMultipleOfTen_isFalse(int minutes) {
        assertFalse(validator.isValid(minutes, null));
    }
}
