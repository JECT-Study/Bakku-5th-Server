package com.ject.post.application.port;

import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;

import java.time.Instant;
import java.util.List;

public interface PostSummaryReader {

    List<Row> findHomePage(Condition condition, Long cursorId, int limit);

    record Condition(Boolean nowExchange, PostCategory category, Instant now) {
    }

    record Row(
            Long id,
            Long authorId,
            String thumbnailKey,
            String title,
            String regionName,
            PostStatus status,
            Instant nowExchangeEndsAt,
            String wantedItem,
            Instant createdAt
    ) {
    }
}
