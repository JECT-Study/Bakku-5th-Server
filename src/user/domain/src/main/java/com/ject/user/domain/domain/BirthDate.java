package com.ject.user.domain.domain;

import com.ject.user.domain.exception.InvalidBirthDateException;
import com.ject.user.domain.exception.UnderAgeException;

import java.time.LocalDate;
import java.time.Period;

/**
 * 생년월일 값 객체. 1900-01-01 이전은 생년월일로 보지 않는다 (결정 A6-3).
 */
public record BirthDate(LocalDate value) {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final int MIN_SIGNUP_AGE = 14;

    public BirthDate {
        if (value == null) {
            throw new InvalidBirthDateException("생년월일은 비어 있을 수 없습니다.");
        }
        if (value.isBefore(MIN_DATE)) {
            throw new InvalidBirthDateException("생년월일은 1900-01-01 이후여야 합니다.");
        }
    }

    /**
     * 가입 가능 나이인지 확인한다. today는 한국 시간(KST) 기준 날짜다 (결정 A6-1).
     * 생일 당일부터 한 살이 늘고, 2월 29일생은 평년에 3월 1일부터 늘어난다 (결정 A6-2 · C11).
     */
    public void ensureSignupAge(LocalDate today) {
        if (value.isAfter(today)) {
            throw new InvalidBirthDateException("생년월일은 미래일 수 없습니다.");
        }
        if (Period.between(value, today).getYears() < MIN_SIGNUP_AGE) {
            throw new UnderAgeException();
        }
    }
}
