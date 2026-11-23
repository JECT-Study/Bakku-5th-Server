package com.ject.exchange.api.controller;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 컨트롤러 테스트용 가짜 글 Port. 글 stub(게시자 100 · 200 고정) 대신 테스트가 게시자를 정한다(spec 가정 5).
 */
class FakePostPort implements PostPort {

    private final Map<Long, PostInfo> posts = new HashMap<>();

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

    // E-1 · E-2 컨트롤러 테스트는 글 상태를 바꾸지 않는다 (약속 확정 · 교환 확인에서만 쓰임)
    @Override
    public void reserve(List<Long> postIds) {
    }

    @Override
    public void complete(List<Long> postIds) {
    }

    @Override
    public void release(List<Long> postIds) {
    }

    void register(PostInfo post) {
        posts.put(post.id(), post);
    }
}
