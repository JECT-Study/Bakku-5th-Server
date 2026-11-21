package com.ject.post.api.controller;

import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.idempotency.Idempotent;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.PostApi;
import com.ject.post.api.dto.request.CreatePostRequest;
import com.ject.post.api.dto.request.UpdatePostRequest;
import com.ject.post.api.dto.response.PostResponse;
import com.ject.post.api.dto.response.RelatedChatResponse;
import com.ject.post.application.dto.PostResult;
import com.ject.post.application.dto.command.IncreaseViewCountCommand;
import com.ject.post.application.dto.query.GetPostQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
    public ApiResponse<PostResponse> getPost(Long memberId, Long postId) {
        commandBus.send(new IncreaseViewCountCommand(postId, memberId));
        PostResult result = queryBus.send(new GetPostQuery(postId, memberId));
        return ApiResponse.success(PostResponse.fromDetail(result, imageUrlResolver));
    }

    @Override
    public ApiResponse<PostResponse> updatePost(Long memberId, Long postId, UpdatePostRequest request) {
        commandBus.send(request.toCommand(postId, memberId));
        PostResult result = queryBus.send(new GetPostQuery(postId, memberId));
        return ApiResponse.success(PostResponse.from(result, imageUrlResolver));
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
}
