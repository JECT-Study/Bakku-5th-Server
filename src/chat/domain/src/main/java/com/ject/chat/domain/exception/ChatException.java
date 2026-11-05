package com.ject.chat.domain.exception;

import com.ject.common.core.exception.DomainException;

public class ChatException extends DomainException {

    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }
}
