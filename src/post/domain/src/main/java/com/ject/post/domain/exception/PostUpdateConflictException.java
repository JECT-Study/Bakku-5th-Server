package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostUpdateConflictException extends DomainException {

    public PostUpdateConflictException(Long postId) {
        super(PostErrorCode.UPDATE_CONFLICT, "다른 변경이 먼저 반영되어 수정할 수 없습니다. id=" + postId);
    }
}
