package com.ject.exchange.api.controller;

import com.ject.exchange.domain.repository.PostLockRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 컨트롤러 테스트용 가짜 글 잠금. 실제 잠금은 DB에서만 의미가 있으므로 잠근 글만 기록한다.
 */
class FakePostLockRepository implements PostLockRepository {

    private final List<List<Long>> lockedPostIds = new ArrayList<>();

    @Override
    public void lockAll(Collection<Long> postIds) {
        lockedPostIds.add(List.copyOf(postIds));
    }

    List<List<Long>> lockedPostIds() {
        return lockedPostIds;
    }
}
