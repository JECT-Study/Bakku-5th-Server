package com.ject.exchange.api.exception;

import com.ject.common.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ject.exchange.api")
public class ExchangeExceptionHandler extends BaseExceptionHandler {
}
