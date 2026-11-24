package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostNotFoundException extends DomainException {

    public PostNotFoundException(Long postId) {
        super(PostErrorCode.POST_NOT_FOUND, "게시글을 찾을 수 없습니다. id=" + postId);
    }
}
