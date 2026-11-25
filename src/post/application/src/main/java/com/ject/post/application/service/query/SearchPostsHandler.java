package com.ject.post.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.dto.PostSearchPageResult;
import com.ject.post.application.dto.PostSummaryResult;
import com.ject.post.application.dto.query.SearchPostsQuery;
import com.ject.post.application.port.BlockRelationPort;
import com.ject.post.application.port.PostSearchReader;
import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.SearchKeyword;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchPostsHandler implements QueryHandler<SearchPostsQuery, PostSearchPageResult> {

    private final PostSearchReader postSearchReader;
    private final BlockRelationPort blockRelationPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public PostSearchPageResult handle(SearchPostsQuery query) {
        SearchKeyword keyword = SearchKeyword.of(query.keyword());
        PostCategory category = query.category() == null ? null : PostCategory.valueOf(query.category());
        Hub hub = query.hub() == null ? null : Hub.valueOf(query.hub());
        Instant now = serviceTime.now();
        PostSearchReader.Cursor cursor = query.cursorId() == null
                ? null
                : new PostSearchReader.Cursor(query.cursorRank(), query.cursorId());

        List<PostSearchReader.Hit> hits = postSearchReader.search(
                new PostSearchReader.Condition(keyword.value(), category, hub, query.nowExchange(), now,
                        blockRelationPort.blockRelatedMemberIds(query.viewerId())),
                cursor, query.size() + 1);
        boolean hasNext = hits.size() > query.size();
        List<PostSearchReader.Hit> page = hasNext ? hits.subList(0, query.size()) : hits;

        List<PostSummaryResult> content = page.stream()
                .map(hit -> PostSummaryResults.from(hit.row(), query.viewerId(), now, serviceTime))
                .toList();
        PostSearchReader.Hit last = hasNext ? page.get(page.size() - 1) : null;
        return new PostSearchPageResult(content,
                last == null ? null : last.rank(),
                last == null ? null : last.row().id(),
                hasNext);
    }
}
