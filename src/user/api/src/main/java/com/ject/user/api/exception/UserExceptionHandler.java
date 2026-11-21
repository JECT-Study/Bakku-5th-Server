package com.ject.user.api.exception;

import com.ject.common.web.exception.BaseExceptionHandler;
import com.ject.common.web.response.ApiResponse;
import com.ject.common.web.response.ApiResponse.ResponseBody;
import com.ject.user.api.dto.response.RejoinNotAvailableResponse;
import com.ject.user.application.exception.RejoinNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ject.user.api")
public class UserExceptionHandler extends BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);

    /**
     * FE 팝업에 "{날짜}부터 가능"을 보여 주도록 data에 다시 가입할 수 있는 시각을 담는다 (A-03-1).
     */
    @ExceptionHandler(RejoinNotAvailableException.class)
    public ApiResponse<RejoinNotAvailableResponse> handleRejoinNotAvailable(RejoinNotAvailableException e) {
        log.warn("[{}] {}", e.getErrorCode().getCode(), e.getMessage());
        return new ApiResponse<>(new ResponseBody<>(false, e.getErrorCode().getCode(), e.getMessage(),
                new RejoinNotAvailableResponse(e.getRejoinAvailableAt()), null),
                HttpStatusCode.valueOf(e.getErrorCode().getStatus()));
    }
}
