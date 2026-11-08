package com.ject.notification.api.exception;

import com.ject.common.web.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ject.notification.api")
public class NotificationExceptionHandler extends BaseExceptionHandler {
}
