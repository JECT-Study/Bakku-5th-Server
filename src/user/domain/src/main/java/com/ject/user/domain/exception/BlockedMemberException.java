package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 내가 차단했거나 나를 차단한 회원의 프로필을 볼 때 (MEMBER_203).
 */
public class BlockedMemberException extends DomainException {

    public BlockedMemberException(Long memberId) {
        super(UserErrorCode.BLOCKED_MEMBER, "차단 관계인 회원입니다. id=" + memberId);
    }
}
