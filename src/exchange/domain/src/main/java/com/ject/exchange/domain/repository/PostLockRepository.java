package com.ject.exchange.domain.repository;

import java.util.Collection;

/**
 * 글 단위 잠금. 같은 글(또는 첨부 글)에 대한 약속 확정이 동시에 처리되지 않도록 트랜잭션이 끝날 때까지 잡는다 (E-05-1)
 */
public interface PostLockRepository {

    /**
     * 글 ID들을 오름차순으로 잠근다. 다른 트랜잭션이 같은 글을 잠그고 있으면 끝날 때까지 기다린다
     */
    void lockAll(Collection<Long> postIds);
}
