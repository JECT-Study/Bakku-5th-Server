package com.ject.exchange.api.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TenMinuteStepValidator implements ConstraintValidator<TenMinuteStep, Integer> {

    private static final int STEP = 10;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || value % STEP == 0;
    }
}
