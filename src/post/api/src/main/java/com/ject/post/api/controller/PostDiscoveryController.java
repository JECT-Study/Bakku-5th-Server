package com.ject.post.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.PostDiscoveryApi;
import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostSort;
import com.ject.post.api.dto.enums.PostStatus;
import com.ject.post.api.dto.response.CursorPageResponse;
import com.ject.post.api.dto.response.MapMarkerResponse;
import com.ject.post.api.dto.response.MapMarkersResponse;
import com.ject.post.api.dto.response.PostCountResponse;
import com.ject.post.api.dto.response.PostSummaryResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.post.api.controller.PostStubData.CREATED_AT;
import static com.ject.post.api.controller.PostStubData.NOW_EXCHANGE_ENDS_AT;
import static com.ject.post.api.controller.PostStubData.POST_ID;

@RestController
public class PostDiscoveryController implements PostDiscoveryApi {

    @Override
    public ApiResponse<CursorPageResponse<PostSummaryResponse>> getPosts(Double lat, Double lng, Boolean nowExchange,
                                                                         PostCategory category, PostSort sort,
                                                                         String cursor, Integer size) {
        return ApiResponse.success(page());
    }

    @Override
    public ApiResponse<CursorPageResponse<PostSummaryResponse>> searchPosts(String keyword, PostCategory category,
                                                                            PostSort sort, String cursor, Integer size) {
        return ApiResponse.success(page());
    }

    @Override
    public ApiResponse<MapMarkersResponse> getMapMarkers(Double swLat, Double swLng, Double neLat, Double neLng,
                                                         Integer zoom, Boolean nowExchange, PostCategory category) {
        return ApiResponse.success(new MapMarkersResponse(List.of(
                new MapMarkerResponse(37.5563, 126.9236, 12),
                new MapMarkerResponse(37.5512, 126.9198, 3))));
    }

    @Override
    public ApiResponse<PostCountResponse> countPosts(Double lat, Double lng, Boolean nowExchange, PostCategory category) {
        return ApiResponse.success(new PostCountResponse(12));
    }

    private static CursorPageResponse<PostSummaryResponse> page() {
        PostSummaryResponse post = new PostSummaryResponse(
                POST_ID, "https://{cdn}/post/10/8f14e45f.jpg", "치이카와 하치와레 가챠 키링", "마포구 동교동",
                PostStatus.AVAILABLE, true, NOW_EXCHANGE_ENDS_AT, "치이카와 가챠 키링 (하치와레, 모몽가 제외)",
                false, CREATED_AT);
        return new CursorPageResponse<>(List.of(post), "eyJpZCI6MTB9", true);
    }
}
