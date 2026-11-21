package com.ject.post.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.dto.PostPageResult;
import com.ject.post.application.dto.PostSummaryResult;
import com.ject.post.application.dto.query.GetPostsQuery;
import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPostsHandler implements QueryHandler<GetPostsQuery, PostPageResult> {

    private final PostSummaryReader postSummaryReader;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public PostPageResult handle(GetPostsQuery query) {
        Instant now = serviceTime.now();
        PostCategory category = query.category() == null ? null : PostCategory.valueOf(query.category());
        PostSummaryReader.Condition condition = new PostSummaryReader.Condition(query.nowExchange(), category, now);

        List<PostSummaryReader.Row> rows = postSummaryReader.findHomePage(condition, query.cursorId(), query.size() + 1);
        boolean hasNext = rows.size() > query.size();
        List<PostSummaryReader.Row> page = hasNext ? rows.subList(0, query.size()) : rows;

        List<PostSummaryResult> content = page.stream()
                .map(row -> toResult(row, query.viewerId(), now))
                .toList();
        Long nextCursorId = hasNext ? page.get(page.size() - 1).id() : null;
        return new PostPageResult(content, nextCursorId, hasNext);
    }

    private PostSummaryResult toResult(PostSummaryReader.Row row, Long viewerId, Instant now) {
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
