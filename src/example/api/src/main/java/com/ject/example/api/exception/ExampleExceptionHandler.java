package com.ject.example.api.exception;

import com.ject.common.exception.DomainException;
import com.ject.common.exception.ErrorType;
import com.ject.example.api.controller.FooController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * example 도메인 컨트롤러에서 발생한 도메인 예외를 HTTP 응답으로 변환한다.
 * api는 domain을 모르므로 common의 DomainException 타입으로 받고, ErrorType으로 상태 코드를 정한다.
 */
@RestControllerAdvice(basePackageClasses = FooController.class)
public class ExampleExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException e) {
        return ProblemDetail.forStatusAndDetail(toHttpStatus(e.getErrorType()), e.getMessage());
    }

    private static HttpStatus toHttpStatus(ErrorType errorType) {
        return switch (errorType) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }
}
