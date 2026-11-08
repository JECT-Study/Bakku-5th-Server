package com.ject.notification.domain.exception;

import com.ject.common.core.exception.ErrorCode;

/**
 * 알림 도메인 에러 코드. 코드 체계는 회원 · 알림 API 명세서의 에러 코드 탭을 따른다.
 */
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND("NOTIFICATION_001", 404, "알림을 찾을 수 없습니다.");

    private final String code;
    private final int status;
    private final String message;

    NotificationErrorCode(String code, int status, String message) {
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
