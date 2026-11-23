package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;
import lombok.Getter;

@Getter
public class PostConflictException extends DomainException {

    private final Long postId;

    private PostConflictException(Long postId, String action) {
        super(PostErrorCode.CONFLICT, "다른 변경이 먼저 반영되어 " + action + "할 수 없습니다. id=" + postId);
        this.postId = postId;
    }

    public static PostConflictException onUpdate(Long postId) {
        return new PostConflictException(postId, "수정");
    }

    public static PostConflictException onDelete(Long postId) {
        return new PostConflictException(postId, "삭제");
    }

    public static PostConflictException onStatusChange(Long postId) {
        return new PostConflictException(postId, "상태를 변경");
    }
}
