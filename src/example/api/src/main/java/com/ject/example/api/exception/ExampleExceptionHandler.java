package com.ject.example.api.exception;

import com.ject.common.exception.BaseExceptionHandler;
import com.ject.example.api.controller.FooController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * example 도메인 컨트롤러에서 발생한 예외를 공통 에러 형식으로 변환한다.
 * 변환 규칙은 common의 BaseExceptionHandler를 그대로 쓰고, 적용 범위만 이 도메인으로 한정한다.
 */
@RestControllerAdvice(basePackageClasses = FooController.class)
public class ExampleExceptionHandler extends BaseExceptionHandler {
}
