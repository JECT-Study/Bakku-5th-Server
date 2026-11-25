package com.ject.post.api.controller.api;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostHub;
import com.ject.post.api.dto.enums.PostSort;
import com.ject.post.api.dto.response.CursorPageResponse;
import com.ject.post.api.dto.response.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Post Discovery", description = "홈 목록 · 검색")
@RequestMapping("/api/v1/posts")
public interface PostDiscoveryApi {

    @Operation(summary = "PO-2 홈 목록", description = "거점 · 교환 방식 · 카테고리로 거른 게시글을 최신순으로 보여줍니다. 교환 가능 게시글만 나옵니다(예약중이 되면 빠졌다가 약속이 취소되면 다시 나옴).")
    @GetMapping
    ApiResponse<CursorPageResponse<PostSummaryResponse>> getPosts(
            @LoginMember Long memberId,
            @Parameter(description = "고른 거점. 생략 시 전체(모든 지역). 거점 밖 게시글은 전체에서만 보임")
            @RequestParam(value = "hub", required = false) PostHub hub,
            @Parameter(description = "true 지금 교환 / false 일반 교환 / 생략 시 모두")
            @RequestParam(value = "nowExchange", required = false) Boolean nowExchange,
            @Parameter(description = "생략 시 전체")
            @RequestParam(value = "category", required = false) PostCategory category,
            @Parameter(description = "기본 LATEST. 지금은 LATEST만 동작")
            @RequestParam(value = "sort", defaultValue = "LATEST") PostSort sort,
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);

    @Operation(summary = "PO-7 제품명 · 태그 검색", description = "제품명 또는 태그에 검색어가 들어간 교환 가능 게시글을 거점 · 교환 방식 · 카테고리로 거르고 정확도순(태그 정확히 일치 → 제품명 포함 → 태그 일부 포함, 같으면 최신순)으로 찾습니다. 띄어쓰기 · 대소문자 · 앞의 #은 무시합니다.")
    @GetMapping("/search")
    ApiResponse<CursorPageResponse<PostSummaryResponse>> searchPosts(
            @LoginMember Long memberId,
            @Parameter(description = "검색어. 띄어쓰기와 앞의 #을 뺀 길이로 1~30자")
            @RequestParam("keyword") @NotBlank @Size(min = 1, max = 30) String keyword,
            @Parameter(description = "생략 시 전체")
            @RequestParam(value = "category", required = false) PostCategory category,
            @Parameter(description = "검색 결과 화면에서 고른 거점. 생략 시 모든 지역. 홈에서 고른 거점과 따로 보냄")
            @RequestParam(value = "hub", required = false) PostHub hub,
            @Parameter(description = "true 지금 교환 / false 일반 교환 / 생략 시 모두")
            @RequestParam(value = "nowExchange", required = false) Boolean nowExchange,
            @Parameter(description = "기본 RELEVANCE(정확도순). 지금은 RELEVANCE만 동작")
            @RequestParam(value = "sort", defaultValue = "RELEVANCE") PostSort sort,
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);
}
