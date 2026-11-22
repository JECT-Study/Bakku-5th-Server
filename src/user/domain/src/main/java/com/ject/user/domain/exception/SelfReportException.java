package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class SelfReportException extends DomainException {

    public SelfReportException(Long memberId) {
        super(ReportErrorCode.SELF_REPORT, "내 글이나 나 자신은 신고할 수 없습니다. id=" + memberId);
    }
}
