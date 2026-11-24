// 출시 후 검토 (E-06-1, #117): 약속 전 알림(분) 10분 단위 검증. 각자 알림 설정과 함께 쓰지 않는다
// package com.ject.exchange.api.dto.validation;
//
// import jakarta.validation.ConstraintValidator;
// import jakarta.validation.ConstraintValidatorContext;
//
// public class TenMinuteStepValidator implements ConstraintValidator<TenMinuteStep, Integer> {
//
//     private static final int STEP = 10;
//
//     @Override
//     public boolean isValid(Integer value, ConstraintValidatorContext context) {
//         return value == null || value % STEP == 0;
//     }
// }
