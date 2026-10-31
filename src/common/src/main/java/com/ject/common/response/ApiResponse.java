package com.ject.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ject.common.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ApiResponse<T> extends ResponseEntity<ApiResponse.ResponseBody<T>> {

    public record ResponseBody<T>(
            boolean success,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String code,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String message,
            T data,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            List<FieldError> errors
    ) {
    }

    public record FieldError(String field, String reason) {
    }

    public ApiResponse(ResponseBody<T> body, HttpStatusCode status) {
        super(body, status);
    }

    public ApiResponse(ResponseBody<T> body, HttpStatusCode status, HttpHeaders headers) {
        super(body, headers, status);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(new ResponseBody<>(true, null, null, data, null), HttpStatus.OK);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(new ResponseBody<>(true, null, null, data, null), HttpStatus.CREATED);
    }

    public static <T> ApiResponse<T> error(ErrorCode code) {
        return error(code, code.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return error(code, message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code, String message, List<FieldError> errors) {
        return new ApiResponse<>(new ResponseBody<>(false, code.getCode(), message, null, errors),
                HttpStatusCode.valueOf(code.getStatus()));
    }
}
