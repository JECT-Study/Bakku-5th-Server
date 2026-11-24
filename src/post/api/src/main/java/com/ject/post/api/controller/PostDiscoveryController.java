package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.PostDiscoveryApi;
import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostHub;
import com.ject.post.api.dto.enums.PostSort;
import com.ject.post.api.dto.enums.PostStatus;
import com.ject.post.api.dto.response.CursorPageResponse;
import com.ject.post.api.dto.response.PostSummaryResponse;
import com.ject.post.application.dto.PostPageResult;
import com.ject.post.application.dto.query.GetPostsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.post.api.controller.PostStubData.CREATED_AT;
import static com.ject.post.api.controller.PostStubData.NOW_EXCHANGE_ENDS_AT;
import static com.ject.post.api.controller.PostStubData.POST_ID;

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
    public ApiResponse<CursorPageResponse<PostSummaryResponse>> searchPosts(String keyword, PostCategory category,
                                                                            PostSort sort, String cursor, Integer size) {
        return ApiResponse.success(page());
    }

    private static CursorPageResponse<PostSummaryResponse> page() {
        PostSummaryResponse post = new PostSummaryResponse(
                POST_ID, "https://{api}/api/v1/images/post/10/8f14e45f.jpg", "치이카와 하치와레 가챠 키링", "서울특별시 마포구 동교동",
                PostStatus.AVAILABLE, true, NOW_EXCHANGE_ENDS_AT, "치이카와 가챠 키링 (하치와레, 모몽가 제외)",
                false, CREATED_AT);
        return new CursorPageResponse<>(List.of(post), "eyJpZCI6MTB9", true);
    }
}
