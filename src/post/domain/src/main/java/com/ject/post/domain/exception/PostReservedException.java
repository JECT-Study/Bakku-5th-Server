package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class PostReservedException extends DomainException {

    private PostReservedException(Long postId, String action) {
        super(PostErrorCode.POST_RESERVED, "약속이 잡힌 글은 " + action + "할 수 없습니다. id=" + postId);
    }

    public static PostReservedException onUpdate(Long postId) {
        return new PostReservedException(postId, "수정");
    }

    public static PostReservedException onDelete(Long postId) {
        return new PostReservedException(postId, "삭제");
    }
}
