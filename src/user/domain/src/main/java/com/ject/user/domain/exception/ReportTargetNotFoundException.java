package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ReportTargetNotFoundException extends DomainException {

    public ReportTargetNotFoundException(String targetType, Long targetId) {
        super(ReportErrorCode.REPORT_TARGET_NOT_FOUND, "신고 대상을 찾을 수 없습니다. " + targetType + " id=" + targetId);
    }
}
