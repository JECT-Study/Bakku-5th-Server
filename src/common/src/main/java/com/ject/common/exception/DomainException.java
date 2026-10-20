package com.ject.common.exception;

/**
 * 모든 도메인 예외의 기반 클래스. 각 도메인은 이를 상속해 도메인 언어로 된 예외를 정의한다.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorType errorType;

    protected DomainException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
