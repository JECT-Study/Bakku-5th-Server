package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 테스트용 가짜 글 Port. 등록한 글만 조회되고, 없는 id는 결과에서 빠진다.
 */
class FakePostPort implements PostPort {

    private final Map<Long, PostInfo> posts = new HashMap<>();
    private final List<List<Long>> reserveRequests = new ArrayList<>();
    private final List<List<Long>> completeRequests = new ArrayList<>();
    private final List<List<Long>> releaseRequests = new ArrayList<>();
    private RuntimeException reserveFailure;

    @Override
    public Optional<PostInfo> findById(Long postId) {
        return Optional.ofNullable(posts.get(postId));
    }

    @Override
    public List<PostInfo> findAllByIds(List<Long> postIds) {
        return postIds.stream()
                .map(posts::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 예약중 요청을 기록한다. 실패를 정해 두면 기록 후 그 예외를 던진다. */
    @Override
    public void reserve(List<Long> postIds) {
        reserveRequests.add(List.copyOf(postIds));
        if (reserveFailure != null) {
            throw reserveFailure;
        }
    }

    /** 교환 완료 요청을 기록한다. */
    @Override
    public void complete(List<Long> postIds) {
        completeRequests.add(List.copyOf(postIds));
    }

    /** 예약 해제 요청을 기록한다. */
    @Override
    public void release(List<Long> postIds) {
        releaseRequests.add(List.copyOf(postIds));
    }

    List<List<Long>> completeRequests() {
        return completeRequests;
    }

    List<List<Long>> releaseRequests() {
        return releaseRequests;
    }

    void failReserveWith(RuntimeException failure) {
        this.reserveFailure = failure;
    }

    List<List<Long>> reserveRequests() {
        return reserveRequests;
    }

    void register(PostInfo post) {
        posts.put(post.id(), post);
    }
}
