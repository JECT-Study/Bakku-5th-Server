package com.ject.post.domain.exception;

import com.ject.common.core.exception.ErrorCode;

public enum PostErrorCode implements ErrorCode {

    POST_NOT_FOUND("POST_001", 404, "삭제됐거나 볼 수 없는 교환글이에요."),
    NOT_POST_AUTHOR("POST_002", 403, "작성자만 할 수 있어요."),
    POST_RESERVED("POST_003", 409, "약속이 잡힌 글은 수정할 수 없어요."),
    POST_COMPLETED("POST_004", 409, "교환이 완료된 글이에요."),
    INVALID_IMAGE_KEY("POST_005", 400, "사용할 수 없는 이미지예요."),
    RESTRICTED_MEMBER("POST_006", 403, "이용이 제한된 회원이에요.");

    private final String code;
    private final int status;
    private final String message;

    PostErrorCode(String code, int status, String message) {
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
