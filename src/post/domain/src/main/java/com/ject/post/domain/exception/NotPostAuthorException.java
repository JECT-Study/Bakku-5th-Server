package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class NotPostAuthorException extends DomainException {

    public NotPostAuthorException(Long postId) {
        super(PostErrorCode.NOT_POST_AUTHOR, "작성자만 수정할 수 있습니다. id=" + postId);
    }
}
