package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class NotPostAuthorException extends DomainException {

    private NotPostAuthorException(Long postId, String action) {
        super(PostErrorCode.NOT_POST_AUTHOR, "작성자만 " + action + "할 수 있습니다. id=" + postId);
    }

    public static NotPostAuthorException onUpdate(Long postId) {
        return new NotPostAuthorException(postId, "수정");
    }

    public static NotPostAuthorException onDelete(Long postId) {
        return new NotPostAuthorException(postId, "삭제");
    }
}
