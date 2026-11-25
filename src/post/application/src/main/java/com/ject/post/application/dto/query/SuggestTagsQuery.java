package com.ject.post.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.post.application.dto.TagSuggestionResult;

public record SuggestTagsQuery(
        String keyword,
        int size
) implements Query<TagSuggestionResult> {
}
