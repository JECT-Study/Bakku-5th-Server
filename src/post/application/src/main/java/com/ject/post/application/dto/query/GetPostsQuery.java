package com.ject.post.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.post.application.dto.PostPageResult;

public record GetPostsQuery(
        Long viewerId,
        Boolean nowExchange,
        String category,
        Long cursorId,
        int size
) implements Query<PostPageResult> {
}
