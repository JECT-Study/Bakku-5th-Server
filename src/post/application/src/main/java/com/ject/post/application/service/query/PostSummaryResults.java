package com.ject.post.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.dto.PostSummaryResult;
import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.Post;

import java.time.Instant;

final class PostSummaryResults {

    private PostSummaryResults() {
    }

    static PostSummaryResult from(PostSummaryReader.Row row, Long viewerId, Instant now, ServiceTime serviceTime) {
        return new PostSummaryResult(
                row.id(),
                row.thumbnailKey(),
                row.title(),
                row.regionName(),
                row.status().name(),
                Post.isNowExchange(row.status(), row.nowExchangeEndsAt(), now),
                serviceTime.toOffset(row.nowExchangeEndsAt()),
                row.wantedItem(),
                row.authorId().equals(viewerId),
                serviceTime.toOffset(row.createdAt()));
    }
}
