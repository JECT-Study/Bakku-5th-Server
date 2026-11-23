package com.ject.exchange.api.dto.response;

import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.dto.enums.PostStatus;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.PostSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 글 요약의 사진 key를 ImageUrlResolver로 URL로 바꾸는지 확인한다 (#107).
 */
class PostSummaryResponseTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");

    @Test
    @DisplayName("사진 key가 있으면 기본 주소 + /api/v1/images/ + key URL이다")
    void from_withKey_resolvesUrl() {
        PostSummaryResponse response = PostSummaryResponse.from(
                new PostSummary(10L, "P", "post/10/1.jpg", PostInfo.Status.AVAILABLE), IMAGE_URL_RESOLVER);

        assertEquals(new PostSummaryResponse(10L, "P", "https://img.test/api/v1/images/post/10/1.jpg",
                PostStatus.AVAILABLE), response);
    }

    @Test
    @DisplayName("사진 key가 null이면 thumbnailUrl은 null이다")
    void from_nullKey_returnsNullUrl() {
        PostSummaryResponse response = PostSummaryResponse.from(
                new PostSummary(21L, "Q2", null, PostInfo.Status.RESERVED), IMAGE_URL_RESOLVER);

        assertNull(response.thumbnailUrl());
        assertEquals(PostStatus.RESERVED, response.status());
    }

    @Test
    @DisplayName("삭제된 글 요약은 상태 DELETED · 제목 · 사진 null로 바뀐다")
    void from_deleted_returnsDeletedWithNulls() {
        PostSummaryResponse response = PostSummaryResponse.from(
                new PostSummary(10L, null, null, PostInfo.Status.DELETED), IMAGE_URL_RESOLVER);

        assertEquals(new PostSummaryResponse(10L, null, null, PostStatus.DELETED), response);
    }

    @Test
    @DisplayName("요약이 null이면 응답도 null이다")
    void from_nullSummary_returnsNull() {
        assertNull(PostSummaryResponse.from(null, IMAGE_URL_RESOLVER));
    }
}
