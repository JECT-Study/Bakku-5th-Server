package com.ject.image.api.exception;

import com.ject.common.web.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ject.image.api")
public class ImageExceptionHandler extends BaseExceptionHandler {
}
