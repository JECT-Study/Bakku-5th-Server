package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 지금 닉네임과 똑같은 닉네임으로 수정 (MEMBER_104). FE는 바뀐 내용이 없으면 [저장]을 막는다 (G-03-2).
 */
public class SameNicknameException extends DomainException {

    public SameNicknameException() {
        super(UserErrorCode.SAME_NICKNAME);
    }
}
