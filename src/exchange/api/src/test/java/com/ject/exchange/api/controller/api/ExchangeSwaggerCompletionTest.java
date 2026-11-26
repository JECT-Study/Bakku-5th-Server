package com.ject.exchange.api.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeSwaggerCompletionTest {

    private static final List<Class<?>> EXCHANGE_API_TYPES = List.of(ExchangeApi.class, AppointmentApi.class);

    @Test
    @DisplayName("노출된 교환 API 11개는 Swagger 요약에 완료 표시가 있다")
    void exposedOperations_haveCompletionMark() {
        List<Method> operations = EXCHANGE_API_TYPES.stream()
                .flatMap(apiType -> Arrays.stream(apiType.getDeclaredMethods()))
                .toList();

        assertEquals(11, operations.size());
        assertAll(operations.stream().map(method -> () -> {
            Operation operation = method.getAnnotation(Operation.class);
            assertTrue(operation != null && operation.summary().startsWith("✅ "),
                    () -> method.getName() + " Swagger 요약에 완료 표시가 없다");
        }));
    }
}
