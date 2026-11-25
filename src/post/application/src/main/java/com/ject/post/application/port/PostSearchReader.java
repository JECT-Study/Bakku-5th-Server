package com.ject.post.application.port;

import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.PostCategory;

import java.time.Instant;
import java.util.List;

public interface PostSearchReader {

    // 정확도순(rank 1 태그 정확 · 2 제품명 포함 · 3 태그 일부) → 같은 rank는 최신순
    List<Hit> search(Condition condition, Cursor cursor, int limit);

    record Condition(String keyword, PostCategory category, Hub hub, Boolean nowExchange, Instant now,
                     List<Long> excludedAuthorIds) {
    }

    record Cursor(int rank, Long id) {
    }

    record Hit(int rank, PostSummaryReader.Row row) {
    }
}
