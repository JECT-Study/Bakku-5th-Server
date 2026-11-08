package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

public class InvalidNicknameException extends DomainException {

    public InvalidNicknameException(String message) {
        super(UserErrorCode.INVALID_NICKNAME, message);
    }
}
