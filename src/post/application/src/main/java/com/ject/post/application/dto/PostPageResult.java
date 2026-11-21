package com.ject.post.application.dto;

import java.util.List;

public record PostPageResult(
        List<PostSummaryResult> content,
        Long nextCursorId,
        boolean hasNext
) {
}
