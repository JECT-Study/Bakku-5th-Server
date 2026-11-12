package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 영구 정지 회원은 로그인 · 모든 인증 요청이 막힌다 (MEMBER_003, A-03-1 · A-09-1).
 */
public class MemberBannedException extends DomainException {

    public MemberBannedException() {
        super(UserErrorCode.MEMBER_BANNED);
    }
}
