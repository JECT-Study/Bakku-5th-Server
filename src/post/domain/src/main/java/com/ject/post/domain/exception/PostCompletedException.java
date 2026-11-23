package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostCompletedException extends DomainException {

    private PostCompletedException(Long postId, String action) {
        super(PostErrorCode.POST_COMPLETED, "교환이 완료된 글은 " + action + "할 수 없습니다. id=" + postId);
    }

    public static PostCompletedException onUpdate(Long postId) {
        return new PostCompletedException(postId, "수정");
    }

    public static PostCompletedException onDelete(Long postId) {
        return new PostCompletedException(postId, "삭제");
    }
}
