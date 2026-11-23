package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.application.port.PostStatusRejectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 테스트용 가짜 글 Port. 등록한 글만 조회되고, 없는 id는 결과에서 빠진다.
 * post BC처럼 예약중 · 완료 · 해제 요청이 등록한 글의 상태를 바꾼다. 거부할 글을 정해 두면 아무것도 바꾸지 않고 거부한다.
 */
class FakePostPort implements PostPort {

    private final Map<Long, PostInfo> posts = new HashMap<>();
    private final List<List<Long>> reserveRequests = new ArrayList<>();
    private final List<List<Long>> completeRequests = new ArrayList<>();
    private final List<List<Long>> releaseRequests = new ArrayList<>();
    private final Set<Long> rejectedPostIds = new HashSet<>();
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
        changeStatus(postIds, PostInfo.Status.RESERVED);
    }

    /** 교환 완료 요청을 기록한다. */
    @Override
    public void complete(List<Long> postIds) {
        completeRequests.add(List.copyOf(postIds));
        changeStatus(postIds, PostInfo.Status.COMPLETED);
    }

    /** 예약 해제 요청을 기록한다. */
    @Override
    public void release(List<Long> postIds) {
        releaseRequests.add(List.copyOf(postIds));
        changeStatus(postIds, PostInfo.Status.AVAILABLE);
    }

    /** post BC처럼 하나라도 거부되면 아무 글도 바꾸지 않는다. */
    private void changeStatus(List<Long> postIds, PostInfo.Status target) {
        postIds.stream()
                .filter(rejectedPostIds::contains)
                .findFirst()
                .ifPresent(rejectedPostId -> {
                    throw new PostStatusRejectedException(rejectedPostId);
                });
        postIds.forEach(postId -> posts.computeIfPresent(postId, (id, post) ->
                new PostInfo(post.id(), post.title(), post.thumbnailKey(), post.ownerId(), target)));
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

    /** 이 글이 포함된 예약중 · 완료 · 해제 요청을 post BC가 거부한다. */
    void rejectStatusChangeOf(Long postId) {
        rejectedPostIds.add(postId);
    }

    List<List<Long>> reserveRequests() {
        return reserveRequests;
    }

    PostInfo.Status statusOf(Long postId) {
        return posts.get(postId).status();
    }

    void register(PostInfo post) {
        posts.put(post.id(), post);
    }
}
