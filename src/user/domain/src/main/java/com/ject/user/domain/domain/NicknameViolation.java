package com.ject.user.domain.domain;

/**
 * 닉네임 규칙 위반 종류. 화면 안내 문구가 글자 수 · 형식으로 나뉜다 (A-05-2, 11/12 변경).
 */
public enum NicknameViolation {

    INVALID_LENGTH("닉네임은 2~10자로 입력해주세요."),
    INVALID_FORMAT("닉네임은 띄어쓰기 없이 한글, 영문, 숫자만 가능해요.");

    private final String message;

    NicknameViolation(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
