package com.ject.user.api.exception;

import com.ject.common.web.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ject.user.api")
public class UserExceptionHandler extends BaseExceptionHandler {
}
