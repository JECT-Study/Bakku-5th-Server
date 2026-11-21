package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostCompletedException extends DomainException {

    public PostCompletedException(Long postId) {
        super(PostErrorCode.POST_COMPLETED, "교환이 완료된 글은 수정할 수 없습니다. id=" + postId);
    }
}
