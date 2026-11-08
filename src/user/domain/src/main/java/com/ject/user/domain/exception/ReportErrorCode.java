package com.ject.user.domain.exception;

import com.ject.common.core.exception.ErrorCode;

/**
 * 신고 에러 코드. 신고는 회원 모듈에서 담당한다.
 */
public enum ReportErrorCode implements ErrorCode {

    REPORT_TARGET_NOT_FOUND("REPORT_001", 404, "신고 대상을 찾을 수 없습니다."),
    ALREADY_REPORTED("REPORT_002", 409, "이미 신고한 대상입니다."),
    ITEM_PROBLEM_NOT_ALLOWED("REPORT_003", 400, "교환을 완료한 상대에게만 교환 물품 문제로 신고할 수 있습니다."),
    SELF_REPORT("REPORT_004", 400, "내 글이나 나 자신은 신고할 수 없습니다.");

    private final String code;
    private final int status;
    private final String message;

    ReportErrorCode(String code, int status, String message) {
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
