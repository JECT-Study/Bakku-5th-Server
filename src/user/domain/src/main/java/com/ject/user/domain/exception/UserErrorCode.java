package com.ject.user.domain.exception;

import com.ject.common.core.exception.ErrorCode;

/**
 * 회원 도메인 에러 코드. 코드 체계는 회원 · 알림 API 명세서의 에러 코드 탭을 따른다.
 */
public enum UserErrorCode implements ErrorCode {

    SOCIAL_TOKEN_INVALID("MEMBER_001", 401, "소셜 토큰을 검증하지 못했습니다."),
    REJOIN_NOT_AVAILABLE("MEMBER_002", 403, "탈퇴 후 7일이 지나지 않은 소셜 계정입니다."),
    MEMBER_BANNED("MEMBER_003", 403, "이용이 정지된 계정입니다."),
    SIGNUP_TOKEN_INVALID("MEMBER_010", 401, "가입 토큰이 만료됐거나 올바르지 않습니다."),
    REQUIRED_TERMS_NOT_AGREED("MEMBER_011", 400, "필수 약관에 동의하지 않았습니다."),
    TERMS_VERSION_OUTDATED("MEMBER_012", 400, "약관 버전이 최신이 아닙니다."),
    UNDER_AGE("MEMBER_013", 400, "만 14세 이상만 가입할 수 있습니다."),
    ALREADY_SIGNED_UP("MEMBER_014", 409, "이미 가입된 소셜 계정입니다."),
    INVALID_NICKNAME("MEMBER_101", 400, "닉네임 형식이 올바르지 않습니다."),
    DUPLICATE_NICKNAME("MEMBER_102", 409, "이미 사용 중인 닉네임입니다."),
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
