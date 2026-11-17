package com.ject.post.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.post.application.dto.PostResult;

public record GetPostQuery(Long postId, Long viewerId) implements Query<PostResult> {
}
