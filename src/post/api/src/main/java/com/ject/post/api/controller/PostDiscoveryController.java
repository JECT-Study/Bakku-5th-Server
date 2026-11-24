package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.PostDiscoveryApi;
import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostHub;
import com.ject.post.api.dto.enums.PostSort;
import com.ject.post.api.dto.response.CursorPageResponse;
import com.ject.post.api.dto.response.PostSummaryResponse;
import com.ject.post.application.dto.PostPageResult;
import com.ject.post.application.dto.PostSearchPageResult;
import com.ject.post.application.dto.query.GetPostsQuery;
import com.ject.post.application.dto.query.SearchPostsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostDiscoveryController implements PostDiscoveryApi {

    private final QueryBus queryBus;
    private final ImageUrlResolver imageUrlResolver;

    @Override
    public ApiResponse<CursorPageResponse<PostSummaryResponse>> getPosts(Long memberId, PostHub hub,
                                                                         Boolean nowExchange, PostCategory category,
                                                                         PostSort sort, String cursor, Integer size) {
        GetPostsQuery query = new GetPostsQuery(memberId, nowExchange,
                category == null ? null : category.name(), hub == null ? null : hub.name(),
                PostCursor.decode(cursor), size);
        PostPageResult result = queryBus.send(query);
        List<PostSummaryResponse> content = result.content().stream()
                .map(summary -> PostSummaryResponse.from(summary, imageUrlResolver))
                .toList();
        return ApiResponse.success(
                new CursorPageResponse<>(content, PostCursor.encode(result.nextCursorId()), result.hasNext()));
    }

    @Override
    public ApiResponse<CursorPageResponse<PostSummaryResponse>> searchPosts(Long memberId, String keyword,
                                                                            PostCategory category, PostSort sort,
                                                                            String cursor, Integer size) {
        PostSearchCursor position = PostSearchCursor.decode(cursor);
        SearchPostsQuery query = new SearchPostsQuery(memberId, keyword, category == null ? null : category.name(),
                position == null ? null : position.rank(), position == null ? null : position.id(), size);
        PostSearchPageResult result = queryBus.send(query);
        List<PostSummaryResponse> content = result.content().stream()
                .map(summary -> PostSummaryResponse.from(summary, imageUrlResolver))
                .toList();
        return ApiResponse.success(new CursorPageResponse<>(content,
                PostSearchCursor.encode(result.nextCursorRank(), result.nextCursorId()), result.hasNext()));
    }
}
