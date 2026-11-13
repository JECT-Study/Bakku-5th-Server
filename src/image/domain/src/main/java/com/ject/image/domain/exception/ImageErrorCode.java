package com.ject.image.domain.exception;

import com.ject.common.core.exception.ErrorCode;

public enum ImageErrorCode implements ErrorCode {

    UNSUPPORTED_IMAGE("IMAGE_001", 400, "올릴 수 없는 이미지입니다."),
    IMAGE_NOT_FOUND("IMAGE_002", 404, "이미지를 찾을 수 없습니다."),
    IMAGE_ACCESS_DENIED("IMAGE_003", 403, "이미지를 볼 권한이 없습니다.");

    private final String code;
    private final int status;
    private final String message;

    ImageErrorCode(String code, int status, String message) {
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
