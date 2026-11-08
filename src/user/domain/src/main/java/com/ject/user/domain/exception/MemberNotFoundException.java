package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class MemberNotFoundException extends DomainException {

    public MemberNotFoundException(Long memberId) {
        super(UserErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다. id=" + memberId);
    }
}
