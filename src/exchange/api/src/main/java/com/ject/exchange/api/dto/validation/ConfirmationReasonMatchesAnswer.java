package com.ject.exchange.api.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 교환 확인 응답의 답과 이유 조합을 검증한다. 못 만났어요는 이유 필수, 교환했어요는 이유 없음, 직접 입력은 기타 이유에만.
 * 필드 하나로는 판정할 수 없어 record 단위로 두고, 오류는 해당 필드에 붙여 COMMON_001 + errors[].field로 나간다 (api 규약)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConfirmationReasonMatchesAnswerValidator.class)
public @interface ConfirmationReasonMatchesAnswer {

    String message() default "교환 확인 응답과 이유가 맞지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
