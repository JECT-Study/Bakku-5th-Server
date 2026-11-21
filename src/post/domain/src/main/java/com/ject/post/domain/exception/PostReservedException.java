package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostReservedException extends DomainException {

    public PostReservedException(Long postId) {
        super(PostErrorCode.POST_RESERVED, "약속이 잡힌 글은 수정할 수 없습니다. id=" + postId);
    }
}
