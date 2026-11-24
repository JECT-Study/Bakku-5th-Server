package com.ject.post.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.post.application.dto.PostSearchPageResult;

public record SearchPostsQuery(
        Long viewerId,
        String keyword,
        String category,
        Integer cursorRank,
        Long cursorId,
        int size
) implements Query<PostSearchPageResult> {
}
