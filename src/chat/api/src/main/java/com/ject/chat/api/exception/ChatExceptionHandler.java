package com.ject.chat.api.exception;

import com.ject.common.web.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ject.chat.api")
public class ChatExceptionHandler extends BaseExceptionHandler {
}
