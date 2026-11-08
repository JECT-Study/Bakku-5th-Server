package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.WishlistApi;
import com.ject.user.api.dto.request.WishlistKeywordRequest;
import com.ject.user.api.dto.response.WishlistKeywordCreateResponse;
import com.ject.user.api.dto.response.WishlistKeywordListResponse;
import com.ject.user.api.dto.response.WishlistKeywordResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WishlistController implements WishlistApi {

    private static final int MAX_COUNT = 10;

    @Override
    public ApiResponse<WishlistKeywordListResponse> getKeywords() {
        return ApiResponse.success(new WishlistKeywordListResponse(
                List.of(new WishlistKeywordResponse(1L, "치이카와")), MAX_COUNT, true));
    }

    @Override
    public ApiResponse<WishlistKeywordCreateResponse> addKeyword(WishlistKeywordRequest request) {
        return ApiResponse.created(new WishlistKeywordCreateResponse(2L, request.keyword(), false));
    }

    @Override
    public ApiResponse<Void> deleteKeyword(Long keywordId) {
        return ApiResponse.success(null);
    }
}
