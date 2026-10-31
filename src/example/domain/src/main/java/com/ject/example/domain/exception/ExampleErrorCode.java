package com.ject.example.domain.exception;

import com.ject.common.exception.ErrorCode;

/**
 * example 도메인 에러 코드. 도메인마다 이처럼 enum 하나에 code · HTTP 상태 · 기본 문구를 모은다
 */
public enum ExampleErrorCode implements ErrorCode {

    FOO_NOT_FOUND("EXAMPLE_001", 404, "Foo를 찾을 수 없습니다."),
    INVALID_FOO_NAME("EXAMPLE_002", 400, "Foo 이름이 올바르지 않습니다."),
    FOO_ARCHIVED("EXAMPLE_003", 409, "보관된 Foo는 수정할 수 없습니다."),
    BAR_LIMIT_EXCEEDED("EXAMPLE_004", 409, "Bar를 더 추가할 수 없습니다."),
    INVALID_BAR_CONTENT("EXAMPLE_005", 400, "Bar 내용이 올바르지 않습니다.");

    private final String code;
    private final int status;
    private final String message;

    ExampleErrorCode(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
