package com.ject.common.exception;

import com.ject.common.response.ApiResponse;
import com.ject.common.response.ApiResponse.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

public abstract class BaseExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(DomainException.class)
    public ApiResponse<Void> handleDomainException(DomainException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{}] {}", errorCode.getCode(), e.getMessage());
        return ApiResponse.error(errorCode, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleBodyValidation(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return invalidInput(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ApiResponse<Void> handleParameterValidation(HandlerMethodValidationException e) {
        List<FieldError> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> {
                    if (result instanceof ParameterErrors parameterErrors) {
                        return parameterErrors.getFieldErrors().stream()
                                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()));
                    }
                    String name = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors().stream()
                            .map(error -> new FieldError(name, error.getDefaultMessage()));
                })
                .toList();
        return invalidInput(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return invalidInput(List.of(new FieldError(e.getName(), "형식이 올바르지 않습니다.")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        return invalidInput(List.of(new FieldError(e.getParameterName(), "필수 값입니다.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ApiResponse.error(CommonErrorCode.UNREADABLE_BODY);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception e) throws Exception {
        if (e instanceof ErrorResponse) {
            throw e;
        }
        log.error("처리되지 않은 예외", e);
        return ApiResponse.error(CommonErrorCode.INTERNAL_ERROR);
    }

    private static ApiResponse<Void> invalidInput(List<FieldError> errors) {
        CommonErrorCode errorCode = CommonErrorCode.INVALID_INPUT;
        return ApiResponse.error(errorCode, errorCode.getMessage(), errors);
    }
}
