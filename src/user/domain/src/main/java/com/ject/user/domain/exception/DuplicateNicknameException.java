package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 다른 회원이 이미 쓰는 닉네임 (MEMBER_102). 닉네임 확인 뒤 다른 사람이 먼저 쓴 경우도 포함한다 (결정 C8).
 */
public class DuplicateNicknameException extends DomainException {

    public DuplicateNicknameException() {
        super(UserErrorCode.DUPLICATE_NICKNAME);
    }
}
