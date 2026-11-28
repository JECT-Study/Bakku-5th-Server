package com.ject.chat.api.exception;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;

/**
 * CH-3 메시지 목록에서 cursor와 after를 함께 보낸 요청
 */
public class CursorAfterConflictException extends DomainException {

    public CursorAfterConflictException() {
        super(CommonErrorCode.INVALID_INPUT, "cursor와 after는 함께 쓸 수 없습니다.");
    }
}
