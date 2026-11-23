package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostStatusTransitionException extends DomainException {

    public PostStatusTransitionException(Long postId) {
        super(PostErrorCode.INVALID_STATUS, "지금 상태에서는 바꿀 수 없는 글입니다. id=" + postId);
    }
}
