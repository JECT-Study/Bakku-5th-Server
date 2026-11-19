package com.ject.user.domain.domain;

import com.ject.user.domain.exception.InvalidNicknameException;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 닉네임 값 객체. 2~10자, 완성형 한글(가~힣) · 영문 · 숫자만 허용한다 (A-05-2, G-03-2, 결정 A5-0 · A5-1).
 * 앞뒤 공백은 FE가 정리하므로 서버는 정리하지 않고 형식 오류로 본다 (결정 A5-3).
 */
public record Nickname(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 10;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[가-힣a-zA-Z0-9]*$");

    public Nickname {
        validate(value).ifPresent(violation -> {
            throw new InvalidNicknameException(violation.getMessage());
        });
    }

    /**
     * 쓸 수 없는 글자가 있으면 글자 수와 관계없이 형식 오류로 본다. 글자 수만 맞춰서는 고칠 수 없어서다.
     */
    public static Optional<NicknameViolation> validate(String value) {
        if (value == null) {
            return Optional.of(NicknameViolation.INVALID_LENGTH);
        }
        if (!ALLOWED_CHARACTERS.matcher(value).matches()) {
            return Optional.of(NicknameViolation.INVALID_FORMAT);
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return Optional.of(NicknameViolation.INVALID_LENGTH);
        }
        return Optional.empty();
    }

    /**
     * 중복 검사용 값. 영문 대소문자를 구분하지 않고 비교한다 (결정 A5-2). 저장 · 표시는 value 그대로 쓴다.
     */
    public String normalized() {
        return value.toLowerCase(Locale.ROOT);
    }
}
