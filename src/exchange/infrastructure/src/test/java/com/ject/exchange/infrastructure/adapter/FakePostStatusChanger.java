package com.ject.exchange.infrastructure.adapter;

import com.ject.post.application.contract.PostStatusChangeRejectedException;
import com.ject.post.application.contract.PostStatusChanger;

import java.util.ArrayList;
import java.util.List;

/**
 * 테스트용 가짜 글 상태 변경. 받은 요청을 기록하고, 거부를 정해 두면 post 계약의 거부 예외를 던진다.
 */
class FakePostStatusChanger implements PostStatusChanger {

    private final List<List<Long>> reserveCalls = new ArrayList<>();
    private final List<List<Long>> completeCalls = new ArrayList<>();
    private final List<List<Long>> releaseCalls = new ArrayList<>();
    private PostStatusChangeRejectedException rejection;

    @Override
    public void reserve(List<Long> postIds) {
        reserveCalls.add(List.copyOf(postIds));
        throwIfRejected();
    }

    @Override
    public void release(List<Long> postIds) {
        releaseCalls.add(List.copyOf(postIds));
        throwIfRejected();
    }

    @Override
    public void complete(List<Long> postIds) {
        completeCalls.add(List.copyOf(postIds));
        throwIfRejected();
    }

    private void throwIfRejected() {
        if (rejection != null) {
            throw rejection;
        }
    }

    void rejectWith(PostStatusChangeRejectedException rejection) {
        this.rejection = rejection;
    }

    List<List<Long>> reserveCalls() {
        return reserveCalls;
    }

    List<List<Long>> completeCalls() {
        return completeCalls;
    }

    List<List<Long>> releaseCalls() {
        return releaseCalls;
    }
}
