package com.ject.post.application.contract;

import lombok.Getter;

@Getter
public class PostStatusChangeRejectedException extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        INVALID_STATUS,
        CONFLICT
    }

    private final Long postId;
    private final Reason reason;

    public PostStatusChangeRejectedException(Long postId, Reason reason, Throwable cause) {
        super("교환글 상태를 바꿀 수 없습니다. postId=" + postId + " reason=" + reason, cause);
        this.postId = postId;
        this.reason = reason;
    }
}
