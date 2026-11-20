package com.ject.post.api.controller;

import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.idempotency.Idempotent;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.PostApi;
import com.ject.post.api.dto.enums.PostStatus;
import com.ject.post.api.dto.request.CreatePostRequest;
import com.ject.post.api.dto.request.PostContentRequest;
import com.ject.post.api.dto.request.UpdatePostRequest;
import com.ject.post.api.dto.response.PostResponse;
import com.ject.post.api.dto.response.RelatedChatResponse;
import com.ject.post.application.dto.PostResult;
import com.ject.post.application.dto.query.GetPostQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.ject.post.api.controller.PostStubData.CREATED_AT;
import static com.ject.post.api.controller.PostStubData.NOW_EXCHANGE_ENDS_AT;

@RestController
@RequiredArgsConstructor
public class PostController implements PostApi {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final ImageUrlResolver imageUrlResolver;

    @Override
    @Idempotent(ttlHours = 12)
    public ApiResponse<PostResponse> createPost(Long memberId, UUID idempotencyKey, CreatePostRequest request) {
        Long postId = commandBus.send(request.toCommand(memberId));
        PostResult result = queryBus.send(new GetPostQuery(postId, memberId));
        return ApiResponse.created(PostResponse.from(result, imageUrlResolver));
    }

    @Override
    public ApiResponse<PostResponse> getPost(Long postId) {
        return ApiResponse.success(PostStubData.post(postId, false, true));
    }

    @Override
    public ApiResponse<PostResponse> updatePost(Long postId, UpdatePostRequest request) {
        return ApiResponse.success(fromRequest(postId, request, NOW_EXCHANGE_ENDS_AT, 3, 41));
    }

    @Override
    public ApiResponse<Void> deletePost(Long postId) {
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<RelatedChatResponse> getRelatedChats(Long postId) {
        return ApiResponse.success(new RelatedChatResponse(true));
    }

    @Override
    public ApiResponse<Void> turnOffNowExchange(Long postId) {
        return ApiResponse.success(null);
    }

    private static PostResponse fromRequest(Long postId, PostContentRequest request,
                                            OffsetDateTime nowExchangeEndsAt, int requestCount, int viewCount) {
        return new PostResponse(
                postId, PostStatus.AVAILABLE,
                PostStubData.images(postId, request.imageKeys()),
                request.title(), request.category(), request.opened(),
                request.tags() == null ? List.of() : request.tags(),
                request.description(), request.wantedItem(),
                PostStubData.place(request.place()),
                nowExchangeEndsAt != null, nowExchangeEndsAt,
                requestCount, viewCount, PostStubData.author(), true, null, null, CREATED_AT);
    }
}
