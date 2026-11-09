package com.ject.common.core.exception;

public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT("COMMON_001", 400, "입력값이 올바르지 않습니다."),
    UNREADABLE_BODY("COMMON_002", 400, "요청 본문을 읽을 수 없습니다."),
    TOO_MANY_REQUESTS("COMMON_003", 429, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    TOKEN_MISSING("AUTH_001", 401, "인증 토큰이 없습니다."),
    TOKEN_EXPIRED("AUTH_002", 401, "인증 토큰이 만료되었습니다."),
    TOKEN_INVALID("AUTH_003", 401, "유효하지 않은 인증 토큰입니다."),
    REFRESH_TOKEN_INVALID("AUTH_004", 401, "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_ALREADY_REISSUED("AUTH_005", 409, "이미 재발급된 토큰입니다."),
    INTERNAL_ERROR("INTERNAL_001", 500, "서버 내부 오류가 발생했습니다.");

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
