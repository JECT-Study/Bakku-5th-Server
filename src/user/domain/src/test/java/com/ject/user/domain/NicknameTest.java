package com.ject.user.domain;

import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.NicknameViolation;
import com.ject.user.domain.exception.InvalidNicknameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NicknameTest {

    @ParameterizedTest
    @ValueSource(strings = {"바꾸", "가챠러버123", "Bakku", "12", "가a1", "abcdefghij"})
    @DisplayName("2~10자의 완성형 한글 · 영문 · 숫자는 닉네임으로 쓸 수 있다")
    void acceptsValidFormat(String value) {
        // when
        Nickname nickname = new Nickname(value);

        // then
        assertEquals(value, nickname.value());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "가", "abcdefghijk", "바 꾸", " 바꾸", "바꾸 ", "바꾸!", "ㅂㅏ꾸", "ㅋㅋ", "bakku_1"})
    @DisplayName("길이 · 공백 · 특수문자 · 자음/모음 단독 규칙을 어기면 만들 수 없다")
    void rejectsInvalidFormat(String value) {
        assertThrows(InvalidNicknameException.class, () -> new Nickname(value));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "가", "abcdefghijk", "열한글자닉네임입니다요"})
    @DisplayName("허용 글자만 썼는데 2~10자가 아니면 글자 수 오류다")
    void lengthViolation(String value) {
        assertEquals(Optional.of(NicknameViolation.INVALID_LENGTH), Nickname.validate(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"바 꾸", " 바꾸", "바꾸!", "ㅋㅋ", "bakku_1", "!", "띄어 쓰기가 있는 아주 긴 닉네임"})
    @DisplayName("허용하지 않는 글자가 있으면 글자 수와 관계없이 형식 오류다")
    void formatViolation(String value) {
        assertEquals(Optional.of(NicknameViolation.INVALID_FORMAT), Nickname.validate(value));
    }

    @Test
    @DisplayName("규칙에 맞으면 위반이 없다")
    void noViolation() {
        assertEquals(Optional.empty(), Nickname.validate("바꾸12"));
    }

    @Test
    @DisplayName("중복 검사용 값은 영문 대소문자를 구분하지 않고, 표시 값은 입력 그대로다")
    void normalizedIgnoresCase() {
        // given
        Nickname upper = new Nickname("Bakku");
        Nickname lower = new Nickname("bakku");

        // when & then
        assertEquals(upper.normalized(), lower.normalized());
        assertEquals("Bakku", upper.value());
    }
}
