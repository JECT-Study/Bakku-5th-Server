package com.ject.post.api.controller;

import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostStatus;
import com.ject.post.api.dto.request.PlaceRequest;
import com.ject.post.api.dto.response.AuthorResponse;
import com.ject.post.api.dto.response.ImageResponse;
import com.ject.post.api.dto.response.PlaceResponse;
import com.ject.post.api.dto.response.PostResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

final class PostStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final String CDN = "https://{cdn}/";

    static final Long POST_ID = 10L;
    static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 11, 2, 14, 30, 0, 0, KST);
    static final OffsetDateTime NOW_EXCHANGE_ENDS_AT = CREATED_AT.plusMinutes(60);

    private PostStubData() {
    }

    static AuthorResponse author() {
        return new AuthorResponse(3L, "하치와레러버", 12);
    }

    static ImageResponse image(Long postId, String uploadedKey) {
        String fileName = uploadedKey.substring(uploadedKey.lastIndexOf('/') + 1);
        String key = "post/" + postId + "/" + fileName;
        return new ImageResponse(key, CDN + key);
    }

    static List<ImageResponse> images(Long postId, List<String> uploadedKeys) {
        return uploadedKeys.stream().map(key -> image(postId, key)).toList();
    }

    /**
     * 요청의 "서울특별시 마포구 동교동"에서 시/도를 뺀 "마포구 동교동"을 응답한다.
     */
    static PlaceResponse place(PlaceRequest place) {
        String regionName = place.regionName().strip();
        int firstSpace = regionName.indexOf(' ');
        String withoutProvince = firstSpace < 0 ? regionName : regionName.substring(firstSpace + 1);
        return new PlaceResponse(place.lat(), place.lng(), place.name(), withoutProvince, place.memo());
    }

    static PostResponse post(Long postId, boolean isMine, Boolean canRequest) {
        return new PostResponse(
                postId, PostStatus.AVAILABLE,
                List.of(image(postId, "8f14e45f.jpg")),
                "치이카와 하치와레 가챠 키링", PostCategory.CHARACTER, false,
                List.of("치이카와", "하치와레"), "치이카와 가챠 3탄에서 뽑았어요.",
                "치이카와 가챠 키링 (하치와레, 모몽가 제외)",
                new PlaceResponse(37.5563, 126.9236, "스타벅스 홍대동교점", "마포구 동교동", "AK플라자 주변"),
                true, NOW_EXCHANGE_ENDS_AT, 3, 41, author(), isMine, null, canRequest, CREATED_AT);
    }
}
