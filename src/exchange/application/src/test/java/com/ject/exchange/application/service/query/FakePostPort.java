package com.ject.exchange.application.service.query;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;

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

    void register(PostInfo post) {
        posts.put(post.id(), post);
    }
}
