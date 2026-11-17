package com.ject.image.api.exception;

import com.ject.common.core.exception.DomainException;
import com.ject.common.web.response.ApiResponse;
import com.ject.image.application.exception.ImageExceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ImageUploadLimitHandler {

    private final long maxMegabytes;

    public ImageUploadLimitHandler(@Value("${spring.servlet.multipart.max-request-size}") DataSize maxRequestSize) {
        this.maxMegabytes = maxRequestSize.toMegabytes();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        DomainException exception = ImageExceptions.requestTooLarge(maxMegabytes);
        return ApiResponse.error(exception.getErrorCode(), exception.getMessage());
    }
}
