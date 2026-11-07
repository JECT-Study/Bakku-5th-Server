package com.ject.post.api.exception;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.web.exception.BaseExceptionHandler;
import com.ject.common.web.response.ApiResponse;
import com.ject.common.web.response.ApiResponse.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice(basePackages = "com.ject.post.api")
public class PostExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ApiResponse<Void> handleMissingHeader(MissingRequestHeaderException e) {
        CommonErrorCode errorCode = CommonErrorCode.INVALID_INPUT;
        return ApiResponse.error(errorCode, errorCode.getMessage(),
                List.of(new FieldError(e.getHeaderName(), "필수 값입니다.")));
    }
}
