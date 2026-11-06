package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class MemberAlreadyWithdrawnException extends DomainException {

    public MemberAlreadyWithdrawnException(Long memberId) {
        super(UserErrorCode.MEMBER_WITHDRAWN, "이미 탈퇴한 회원입니다. id=" + memberId);
    }
}
