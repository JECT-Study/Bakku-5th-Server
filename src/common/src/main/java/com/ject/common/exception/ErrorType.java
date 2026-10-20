package com.ject.common.exception;

/**
 * 도메인 예외의 의미 분류. HTTP 상태 등 외부 표현으로의 변환은 각 도메인의 api 계층이 담당한다.
 */
public enum ErrorType {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT
}
