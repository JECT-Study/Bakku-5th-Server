package com.ject.post.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.dto.request.CreatePostRequest;
import com.ject.post.api.dto.request.UpdatePostRequest;
import com.ject.post.api.dto.response.PostResponse;
import com.ject.post.api.dto.response.RelatedChatResponse;
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

@Tag(name = "Post", description = "교환글 작성 · 상세 · 수정 · 삭제 · 연관 채팅 · 지금 교환 끄기")
@RequestMapping("/api/v1/posts")
public interface PostApi {

    @Operation(summary = "PO-1 교환글 작성", description = "지금 교환 또는 일반 교환 글을 게시합니다. 같은 Idempotency-Key와 본문으로 재요청하면 처음 응답을 그대로 반환합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<PostResponse> createPost(
            @Parameter(description = "UUID. 재시도할 때 같은 값, 입력을 바꾸면 새 값")
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreatePostRequest request);

    @Operation(summary = "PO-3 교환글 상세", description = "글 정보, 요청 · 조회 수, 하단 버튼 판단 정보를 보여줍니다. 조회 수는 다른 사람 글일 때 계정 + 날짜당 1회 오릅니다.")
    @GetMapping("/{postId}")
    ApiResponse<PostResponse> getPost(@PathVariable("postId") Long postId);

    @Operation(summary = "PO-4 교환글 수정", description = "교환 가능 글만 수정합니다. 전체 교체라 보내지 않은 tags · description · wantedItem · place.memo는 지워집니다.")
    @PutMapping("/{postId}")
    ApiResponse<PostResponse> updatePost(@PathVariable("postId") Long postId,
                                         @Valid @RequestBody UpdatePostRequest request);

    @Operation(summary = "PO-5 교환글 삭제", description = "교환 가능 글만 삭제합니다. FE는 PO-10으로 팝업 문구를 고른 뒤 호출합니다.")
    @DeleteMapping("/{postId}")
    ApiResponse<Void> deletePost(@PathVariable("postId") Long postId);

    @Operation(summary = "PO-10 연관 채팅 확인", description = "삭제 팝업 전에 이 글로 열린 방이나 이 글이 첨부된 방이 있는지 조회합니다.")
    @GetMapping("/{postId}/related-chats")
    ApiResponse<RelatedChatResponse> getRelatedChats(@PathVariable("postId") Long postId);

    @Operation(summary = "PO-6 지금 교환 끄기 (2차)", description = "유지 시간 전에 일반 교환 글로 바꿉니다. 이미 꺼진 글도 200입니다.")
    @PostMapping("/{postId}/now-exchange/off")
    ApiResponse<Void> turnOffNowExchange(@PathVariable("postId") Long postId);
}
