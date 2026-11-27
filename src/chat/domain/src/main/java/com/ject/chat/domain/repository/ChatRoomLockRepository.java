package com.ject.chat.domain.repository;

/**
 * (postId, requesterId) 조합 단위 잠금. 동일 게시글·동일 요청자의 동시 요청을 직렬화한다 (C-07-1)
 */
public interface ChatRoomLockRepository {

    /**
     * 조합 키를 잠근다. 다른 트랜잭션이 같은 조합을 잠그고 있으면 끝날 때까지 기다린다
     */
    void lock(Long postId, Long requesterId);
}
