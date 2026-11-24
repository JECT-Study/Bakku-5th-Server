package com.ject.post.application.dto;

import java.util.List;

public record PostSearchPageResult(
        List<PostSummaryResult> content,
        Integer nextCursorRank,
        Long nextCursorId,
        boolean hasNext
) {
}
