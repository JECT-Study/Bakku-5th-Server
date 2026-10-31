package com.ject.common.exception;

public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT("COMMON_001", 400, "입력값이 올바르지 않습니다."),
    UNREADABLE_BODY("COMMON_002", 400, "요청 본문을 읽을 수 없습니다."),
    TOKEN_MISSING("AUTH_001", 401, "인증 토큰이 없습니다."),
    TOKEN_EXPIRED("AUTH_002", 401, "인증 토큰이 만료되었습니다."),
    TOKEN_INVALID("AUTH_003", 401, "유효하지 않은 인증 토큰입니다."),
    INTERNAL_ERROR("COMMON_500", 500, "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final int status;
    private final String message;

    CommonErrorCode(String code, int status, String message) {
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
