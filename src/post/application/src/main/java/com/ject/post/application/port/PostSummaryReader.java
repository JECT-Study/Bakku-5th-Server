package com.ject.post.application.port;

import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;

import java.time.Instant;
import java.util.List;

public interface PostSummaryReader {

    List<Row> findHomePage(Condition condition, Long cursorId, int limit);

    List<Brief> findBriefs(List<Long> ids);

    record Brief(Long id, Long authorId, PostStatus status, String title, String thumbnailKey) {
    }

    record Condition(Boolean nowExchange, PostCategory category, Hub hub, Instant now, List<Long> excludedAuthorIds) {
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
