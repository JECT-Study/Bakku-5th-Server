// 출시 후 검토 (E-06-1, #117): 약속 전 알림(분) 10분 단위 검증. 각자 알림 설정과 함께 쓰지 않는다
// package com.ject.exchange.api.dto.validation;
//
// import jakarta.validation.Constraint;
// import jakarta.validation.Payload;
//
// import java.lang.annotation.ElementType;
// import java.lang.annotation.Retention;
// import java.lang.annotation.RetentionPolicy;
// import java.lang.annotation.Target;
//
// /**
//  * 분 단위 정수가 10의 배수인지 검증한다. null은 통과 (값이 없으면 끔을 뜻하는 필드가 있어서)
//  * Bean Validation 기본 제약에 배수 검사가 없어 추가했다. 오류는 다른 입력 오류처럼 COMMON_001 + errors[].field로 나간다
//  */
// @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
// @Retention(RetentionPolicy.RUNTIME)
// @Constraint(validatedBy = TenMinuteStepValidator.class)
// public @interface TenMinuteStep {
//     String message() default "10분 단위로 입력해 주세요.";
//     Class<?>[] groups() default {};
//     Class<? extends Payload>[] payload() default {};
// }
