package com.ject.post.domain.domain;

import com.ject.post.domain.exception.InvalidImageKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostTest {

    private static final Long AUTHOR_ID = 3L;
    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");
    private static final Place PLACE =
            new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", "AK플라자 주변");

    @Test
    @DisplayName("작성하면 교환 가능 상태, 조회 수 0으로 시작한다")
    void create_startsAvailable() {
        // when
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), null);

        // then
        assertEquals(PostStatus.AVAILABLE, post.getStatus());
        assertEquals(0, post.getViewCount());
        assertNull(post.getId());
    }

    @Test
    @DisplayName("지금 교환 시간을 주면 종료 시각을 저장하고, 종료 시각 전까지만 지금 교환이다")
    void create_withNowExchange_storesEndsAt() {
        // when
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), 60);

        // then
        Instant endsAt = NOW.plus(Duration.ofMinutes(60));
        assertEquals(endsAt, post.getNowExchangeEndsAt());
        assertTrue(post.isNowExchange(NOW));
        assertFalse(post.isNowExchange(endsAt));
    }

    @Test
    @DisplayName("지금 교환 시간이 없으면 일반 교환이다")
    void create_withoutNowExchange_isNormal() {
        // when
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), null);

        // then
        assertNull(post.getNowExchangeEndsAt());
        assertFalse(post.isNowExchange(NOW));
    }

    @Test
    @DisplayName("저장 후 정식 사진 key로 바꾸고 순서는 받은 그대로다")
    void attachImages_replacesKeys() {
        // given
        Post saved = Post.builder()
                .id(10L)
                .authorId(AUTHOR_ID)
                .status(PostStatus.AVAILABLE)
                .imageKeys(List.of("post/tmp/3/8f14e45f.jpg", "post/tmp/3/c9f0f895.png"))
                .place(PLACE)
                .build();

        // when
        saved.attachImages(List.of("post/10/8f14e45f.jpg", "post/10/c9f0f895.png"));

        // then
        assertEquals(List.of("post/10/8f14e45f.jpg", "post/10/c9f0f895.png"), saved.getImageKeys());
    }

    @Test
    @DisplayName("저장 전에는 사진을 연결할 수 없다")
    void attachImages_beforeSave_throws() {
        // given
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), null);

        // when · then
        assertThrows(IllegalStateException.class, () -> post.attachImages(List.of("post/10/8f14e45f.jpg")));
    }

    @Test
    @DisplayName("사용할 수 없는 사진은 몇 번째인지 담아 POST_005")
    void invalidImageKeyException_message() {
        // when
        InvalidImageKeyException e = new InvalidImageKeyException(2, "post/tmp/7/c9f0f895.jpg");

        // then
        assertEquals("POST_005", e.getErrorCode().getCode());
        assertEquals("2번째 이미지를 사용할 수 없습니다. key=post/tmp/7/c9f0f895.jpg", e.getMessage());
    }

    @Test
    @DisplayName("태그를 보내지 않으면 빈 목록이다")
    void create_withoutTags_isEmpty() {
        // when
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), null, null);

        // then
        assertEquals(List.of(), post.getTags());
    }

    @Test
    @DisplayName("태그는 공백을 지우고 소문자로 저장한다")
    void create_normalizesTags() {
        // when
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), List.of("  ChiiKawa ", "치이카와 가챠"), null);

        // then
        assertEquals(List.of("chiikawa", "치이카와가챠"), post.getTags());
    }

    @Test
    @DisplayName("공백 · 대소문자만 다른 태그는 하나로 합치고 처음 나온 순서를 유지한다")
    void create_mergesSameTags() {
        // when
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"),
                List.of("Chiikawa", "하치와레", "chii kawa", "하치 와레"), null);

        // then
        assertEquals(List.of("chiikawa", "하치와레"), post.getTags());
    }

    private static Post create(List<String> imageKeys, Integer nowExchangeMinutes) {
        return create(imageKeys, null, nowExchangeMinutes);
    }

    private static Post create(List<String> imageKeys, List<String> tags, Integer nowExchangeMinutes) {
        return Post.create(AUTHOR_ID, imageKeys, "치이카와 하치와레 가챠 키링", PostCategory.CHARACTER, false,
                tags, "치이카와 가챠 3탄에서 뽑았어요.", null, PLACE, nowExchangeMinutes, NOW);
    }
}