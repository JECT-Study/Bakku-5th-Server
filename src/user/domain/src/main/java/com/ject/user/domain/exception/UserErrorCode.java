package com.ject.user.domain.exception;

import com.ject.common.core.exception.ErrorCode;

/**
 * 회원 도메인 에러 코드. 코드 체계는 회원 · 알림 API 명세서의 에러 코드 탭을 따른다.
 */
public enum UserErrorCode implements ErrorCode {

    UNDER_AGE("MEMBER_013", 400, "만 14세 이상만 가입할 수 있습니다."),
    INVALID_NICKNAME("MEMBER_101", 400, "닉네임 형식이 올바르지 않습니다."),
    MEMBER_NOT_FOUND("MEMBER_201", 404, "회원을 찾을 수 없습니다."),
    MEMBER_WITHDRAWN("MEMBER_202", 404, "탈퇴한 회원입니다.");

    private final String code;
    private final int status;
    private final String message;

    UserErrorCode(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
