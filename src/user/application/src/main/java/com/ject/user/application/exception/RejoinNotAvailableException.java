package com.ject.user.application.exception;

import com.ject.common.core.exception.DomainException;
import com.ject.user.domain.exception.UserErrorCode;

import java.time.OffsetDateTime;

/**
 * 탈퇴 후 7일이 지나지 않은 소셜 계정 (MEMBER_002). 응답에 다시 가입할 수 있는 시각을 함께 준다 (A-03-1).
 */
public class RejoinNotAvailableException extends DomainException {

    private final OffsetDateTime rejoinAvailableAt;

    public RejoinNotAvailableException(OffsetDateTime rejoinAvailableAt) {
        super(UserErrorCode.REJOIN_NOT_AVAILABLE);
        this.rejoinAvailableAt = rejoinAvailableAt;
    }

    public OffsetDateTime getRejoinAvailableAt() {
        return rejoinAvailableAt;
    }
}
