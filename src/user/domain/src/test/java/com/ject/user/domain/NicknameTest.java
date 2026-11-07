package com.ject.user.domain;

import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.exception.InvalidNicknameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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
