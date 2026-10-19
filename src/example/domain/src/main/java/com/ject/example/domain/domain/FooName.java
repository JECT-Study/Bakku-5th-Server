package com.ject.example.domain.domain;

import com.ject.example.domain.exception.InvalidFooNameException;

/**
 * Value Object: 값 자체로 동등성을 판단하고, 생성 시점에 스스로 유효성을 보장한다.
 */
public record FooName(String value) {

    private static final int MAX_LENGTH = 50;

    public FooName {
        if (value == null || value.isBlank()) {
            throw new InvalidFooNameException("Foo 이름은 비어 있을 수 없습니다.");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFooNameException("Foo 이름은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
        }
    }
}
