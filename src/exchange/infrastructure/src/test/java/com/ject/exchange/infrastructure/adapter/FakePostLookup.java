package com.ject.exchange.infrastructure.adapter;

import com.ject.post.application.contract.PostBrief;
import com.ject.post.application.contract.PostLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 테스트용 가짜 글 조회. post 계약대로 없는 id는 예외 없이 결과에서 빠진다.
 */
class FakePostLookup implements PostLookup {

    private final Map<Long, PostBrief> briefs = new HashMap<>();
    private final List<List<Long>> calls = new ArrayList<>();

    @Override
    public List<PostBrief> findAllByIds(List<Long> postIds) {
        calls.add(List.copyOf(postIds));
        return postIds.stream()
                .map(briefs::get)
                .filter(Objects::nonNull)
                .toList();
    }

    void register(Long id, Long authorId, String status, String title, String thumbnailKey) {
        briefs.put(id, new PostBrief(id, authorId, status, title, thumbnailKey));
    }

    List<List<Long>> calls() {
        return calls;
    }
}
