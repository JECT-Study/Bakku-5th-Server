package com.ject.post.api.controller.api;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.dto.request.CreatePostRequest;
import com.ject.post.api.dto.request.UpdatePostRequest;
import com.ject.post.api.dto.response.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@Tag(name = "Post", description = "게시글 작성 · 상세 · 수정 · 삭제 · 지금 교환 끄기")
@RequestMapping("/api/v1/posts")
public interface PostApi {

    @Operation(summary = "PO-1 게시글 작성", description = "지금 교환 또는 일반 교환 게시글을 올립니다. 같은 Idempotency-Key와 본문으로 재요청하면 처음 응답을 그대로 반환합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<PostResponse> createPost(
            @LoginMember Long memberId,
            @Parameter(description = "UUID. 재시도할 때 같은 값, 입력을 바꾸면 새 값")
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreatePostRequest request);

    @Operation(summary = "PO-3 게시글 상세", description = "게시글 정보, 요청 · 조회 수, 하단 버튼 판단 정보를 보여줍니다. 조회 수는 다른 사람 게시글일 때 계정 + 날짜당 1회 오릅니다.")
    @GetMapping("/{postId}")
    ApiResponse<PostResponse> getPost(@LoginMember Long memberId, @PathVariable("postId") Long postId);

    @Operation(summary = "PO-4 게시글 수정", description = "교환 가능 게시글만 작성자가 수정합니다. 전체 교체라 보내지 않은 tags · description · wantedItem · place.memo는 지워지고, 지금 교환 여부 · 시간은 바뀌지 않습니다.")
    @PutMapping("/{postId}")
    ApiResponse<PostResponse> updatePost(@LoginMember Long memberId,
                                         @PathVariable("postId") Long postId,
                                         @Valid @RequestBody UpdatePostRequest request);

    @Operation(summary = "PO-5 게시글 삭제", description = "교환 가능 게시글만 작성자가 삭제합니다. 이미 삭제한 내 게시글을 다시 삭제해도 200입니다.")
    @DeleteMapping("/{postId}")
    ApiResponse<Void> deletePost(@LoginMember Long memberId, @PathVariable("postId") Long postId);

    @Operation(summary = "PO-6 지금 교환 끄기 (stub)", description = "유지 시간 전에 일반 교환 게시글로 바꿉니다. 이미 꺼진 게시글도 200입니다.")
    @PostMapping("/{postId}/now-exchange/off")
    ApiResponse<Void> turnOffNowExchange(@PathVariable("postId") Long postId);
}
