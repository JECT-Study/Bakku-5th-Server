package com.ject.user.domain;

import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.exception.InvalidBirthDateException;
import com.ject.user.domain.exception.UnderAgeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BirthDateTest {

    @Test
    @DisplayName("1900-01-01 이전이나 빈 생년월일은 만들 수 없다")
    void rejectsOutOfRange() {
        assertThrows(InvalidBirthDateException.class, () -> new BirthDate(LocalDate.of(1899, 12, 31)));
        assertThrows(InvalidBirthDateException.class, () -> new BirthDate(null));
        assertDoesNotThrow(() -> new BirthDate(LocalDate.of(1900, 1, 1)));
    }

    @Test
    @DisplayName("미래 생년월일은 만 14세 미만이 아니라 입력값 오류다")
    void futureBirthDateIsInvalidInput() {
        // given
        LocalDate today = LocalDate.of(2026, 11, 6);
        BirthDate birthDate = new BirthDate(today.plusDays(1));

        // when & then
        assertThrows(InvalidBirthDateException.class, () -> birthDate.ensureSignupAge(today));
    }

    @Test
    @DisplayName("14번째 생일 당일부터 가입할 수 있다")
    void signupFromFourteenthBirthday() {
        // given
        BirthDate birthDate = new BirthDate(LocalDate.of(2012, 11, 6));

        // when & then
        assertThrows(UnderAgeException.class, () -> birthDate.ensureSignupAge(LocalDate.of(2026, 11, 5)));
        assertDoesNotThrow(() -> birthDate.ensureSignupAge(LocalDate.of(2026, 11, 6)));
    }

    @Test
    @DisplayName("2월 29일생은 평년에 3월 1일부터 가입할 수 있다")
    void leapDayBirthday() {
        // given
        BirthDate birthDate = new BirthDate(LocalDate.of(2012, 2, 29));

        // when & then
        assertThrows(UnderAgeException.class, () -> birthDate.ensureSignupAge(LocalDate.of(2026, 2, 28)));
        assertDoesNotThrow(() -> birthDate.ensureSignupAge(LocalDate.of(2026, 3, 1)));
    }
}
