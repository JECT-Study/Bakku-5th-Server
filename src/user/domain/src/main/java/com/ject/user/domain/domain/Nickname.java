package com.ject.user.domain.domain;

import com.ject.user.domain.exception.InvalidNicknameException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 닉네임 값 객체. 2~10자, 완성형 한글(가~힣) · 영문 · 숫자만 허용한다 (A-05-2, G-03-2, 결정 A5-0 · A5-1).
 * 앞뒤 공백은 FE가 정리하므로 서버는 정리하지 않고 형식 오류로 본다 (결정 A5-3).
 */
public record Nickname(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[가-힣a-zA-Z0-9]{2,10}$");

    public Nickname {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidNicknameException("닉네임은 2~10자의 한글 · 영문 · 숫자만 사용할 수 있습니다.");
        }
    }

    /**
     * 중복 검사용 값. 영문 대소문자를 구분하지 않고 비교한다 (결정 A5-2). 저장 · 표시는 value 그대로 쓴다.
     */
    public String normalized() {
        return value.toLowerCase(Locale.ROOT);
    }
}
