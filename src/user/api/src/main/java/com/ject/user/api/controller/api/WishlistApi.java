package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.WishlistKeywordRequest;
import com.ject.user.api.dto.response.WishlistKeywordCreateResponse;
import com.ject.user.api.dto.response.WishlistKeywordListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Wishlist", description = "위시리스트 키워드 (2차)")
@RequestMapping("/api/v1/members/me/wishlist-keywords")
public interface WishlistApi {

    @Operation(summary = "WL-1 위시리스트 키워드 목록 (2차)",
            description = "등록한 키워드 칩과 'N / 10'을 보여줍니다. 키워드 알림 켜짐 여부도 함께 내려줍니다.")
    @GetMapping
    ApiResponse<WishlistKeywordListResponse> getKeywords();

    @Operation(summary = "WL-2 위시리스트 키워드 추가 (2차)",
            description = "2~20자, 최대 10개, 띄어쓰기 · 대소문자를 무시하고 중복 불가. 금지어가 있으면 저장하고 경고만 합니다. "
                    + "에러: MEMBER_501 11개째 추가 · MEMBER_502 이미 등록한 키워드")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<WishlistKeywordCreateResponse> addKeyword(@Valid @RequestBody WishlistKeywordRequest request);

    @Operation(summary = "WL-3 위시리스트 키워드 삭제 (2차)",
            description = "칩 ✕로 삭제합니다. 실패하면 FE가 칩을 되돌립니다. 에러: MEMBER_503 키워드 없음")
    @DeleteMapping("/{keywordId}")
    ApiResponse<Void> deleteKeyword(@Parameter(description = "키워드 ID") @PathVariable("keywordId") Long keywordId);
}
