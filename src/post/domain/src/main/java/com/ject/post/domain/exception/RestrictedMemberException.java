package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class RestrictedMemberException extends DomainException {

    public RestrictedMemberException(Long memberId) {
        super(PostErrorCode.RESTRICTED_MEMBER, "이용이 제한된 회원입니다. memberId=" + memberId);
    }
}
