package com.ject.example.domain;

import com.ject.example.domain.domain.FooName;
import com.ject.example.domain.exception.InvalidFooNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FooNameTest {

    @Test
    @DisplayName("앞뒤 공백은 제거된다")
    void stripsWhitespace() {
        assertEquals("foo", new FooName("  foo  ").value());
    }

    @Test
    @DisplayName("빈 이름은 만들 수 없다")
    void rejectsBlank() {
        assertThrows(InvalidFooNameException.class, () -> new FooName(" "));
    }

    @Test
    @DisplayName("50자를 넘는 이름은 만들 수 없다")
    void rejectsTooLong() {
        assertThrows(InvalidFooNameException.class, () -> new FooName("a".repeat(51)));
    }
}
