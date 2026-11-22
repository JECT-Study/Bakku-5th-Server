package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class SelfBlockException extends DomainException {

    public SelfBlockException(Long memberId) {
        super(UserErrorCode.SELF_BLOCK, "자기 자신은 차단할 수 없습니다. id=" + memberId);
    }
}
