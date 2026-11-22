package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class AlreadyReportedException extends DomainException {

    public AlreadyReportedException(String targetType, Long targetId) {
        super(ReportErrorCode.ALREADY_REPORTED, "이미 신고한 대상입니다. " + targetType + " id=" + targetId);
    }
}
