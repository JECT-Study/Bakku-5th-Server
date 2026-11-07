package com.ject.post.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostSort;
import com.ject.post.api.dto.response.CursorPageResponse;
import com.ject.post.api.dto.response.MapMarkersResponse;
import com.ject.post.api.dto.response.PostCountResponse;
import com.ject.post.api.dto.response.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Post Discovery", description = "홈 목록 · 검색 · 지도 숫자 핀 · 주변 글 수")
@RequestMapping("/api/v1/posts")
public interface PostDiscoveryApi {

    @Operation(summary = "PO-2 홈 목록", description = "교환 방식 · 카테고리로 거른 글을 최신순으로 보여줍니다. 교환 가능 · 예약중 글만 나오고, 탐색 위치 1km 거르기는 2차입니다.")
    @GetMapping
    ApiResponse<CursorPageResponse<PostSummaryResponse>> getPosts(
            @Parameter(description = "탐색 위치 위도. lng와 함께 보냄. 생략하면 모든 지역 (2차)")
            @RequestParam(value = "lat", required = false) @DecimalMin("-90") @DecimalMax("90") Double lat,
            @Parameter(description = "탐색 위치 경도 (2차)")
            @RequestParam(value = "lng", required = false) @DecimalMin("-180") @DecimalMax("180") Double lng,
            @Parameter(description = "true 지금 교환 / false 일반 교환 / 생략 시 모두")
            @RequestParam(value = "nowExchange", required = false) Boolean nowExchange,
            @Parameter(description = "생략 시 전체")
            @RequestParam(value = "category", required = false) PostCategory category,
            @Parameter(description = "기본 LATEST. 1차는 LATEST만 동작")
            @RequestParam(value = "sort", defaultValue = "LATEST") PostSort sort,
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);

    @Operation(summary = "PO-7 제목 · 태그 검색 (2차)", description = "제목 또는 태그에 검색어가 들어간 글을 전체 지역에서 찾습니다. 띄어쓰기 · 대소문자 · 앞의 #은 무시합니다.")
    @GetMapping("/search")
    ApiResponse<CursorPageResponse<PostSummaryResponse>> searchPosts(
            @Parameter(description = "검색어. 띄어쓰기와 앞의 #을 뺀 길이로 2~30자")
            @RequestParam("keyword") @NotBlank @Size(min = 2, max = 30) String keyword,
            @Parameter(description = "생략 시 전체")
            @RequestParam(value = "category", required = false) PostCategory category,
            @Parameter(description = "기본 RELEVANCE. 구현 전에는 LATEST로 출시 가능")
            @RequestParam(value = "sort", defaultValue = "RELEVANCE") PostSort sort,
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);

    @Operation(summary = "PO-8 지도 숫자 핀 (2차)", description = "화면 영역 안의 글을 줌에 맞춰 묶은 숫자 핀을 보여줍니다. 구보다 넓게 축소하면 빈 배열입니다.")
    @GetMapping("/map-markers")
    ApiResponse<MapMarkersResponse> getMapMarkers(
            @Parameter(description = "화면 왼쪽 아래 위도")
            @RequestParam("swLat") @DecimalMin("-90") @DecimalMax("90") Double swLat,
            @Parameter(description = "화면 왼쪽 아래 경도")
            @RequestParam("swLng") @DecimalMin("-180") @DecimalMax("180") Double swLng,
            @Parameter(description = "화면 오른쪽 위 위도. swLat보다 큼")
            @RequestParam("neLat") @DecimalMin("-90") @DecimalMax("90") Double neLat,
            @Parameter(description = "화면 오른쪽 위 경도. swLng보다 큼")
            @RequestParam("neLng") @DecimalMin("-180") @DecimalMax("180") Double neLng,
            @Parameter(description = "지도 줌 레벨. 핀을 묶는 기준")
            @RequestParam("zoom") Integer zoom,
            @Parameter(description = "true 지금 교환 / false 일반 교환 / 생략 시 모두")
            @RequestParam(value = "nowExchange", required = false) Boolean nowExchange,
            @Parameter(description = "생략 시 전체")
            @RequestParam(value = "category", required = false) PostCategory category);

    @Operation(summary = "PO-11 주변 글 수 (2차)", description = "탐색 위치 1km 안의 글 수를 보여줍니다. 지도 하단과 홈 리스트 위에 씁니다.")
    @GetMapping("/count")
    ApiResponse<PostCountResponse> countPosts(
            @Parameter(description = "탐색 위치 위도")
            @RequestParam("lat") @DecimalMin("-90") @DecimalMax("90") Double lat,
            @Parameter(description = "탐색 위치 경도. 반경 1km 고정")
            @RequestParam("lng") @DecimalMin("-180") @DecimalMax("180") Double lng,
            @Parameter(description = "true 지금 교환 / false 일반 교환 / 생략 시 모두")
            @RequestParam(value = "nowExchange", required = false) Boolean nowExchange,
            @Parameter(description = "생략 시 전체. 홈에서만 전달")
            @RequestParam(value = "category", required = false) PostCategory category);
}
