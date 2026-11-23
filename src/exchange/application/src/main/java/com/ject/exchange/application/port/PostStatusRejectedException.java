package com.ject.exchange.application.port;

import lombok.Getter;

/**
 * post BC가 글 상태 변경(예약중 · 완료 · 해제)을 거부했다. 어느 글이 거부됐는지에 따라 Handler가 에러를 고른다
 */
@Getter
public class PostStatusRejectedException extends RuntimeException {

    private final Long postId;

    public PostStatusRejectedException(Long postId) {
        super("글 상태를 바꿀 수 없습니다. postId=" + postId);
        this.postId = postId;
    }
}
