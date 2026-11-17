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
    @DisplayName("교환 가능한 다른 사람 글에만 교환 요청할 수 있다")
    void canBeRequestedBy() {
        // given
        Post available = withStatus(PostStatus.AVAILABLE);

        // then
        assertTrue(available.canBeRequestedBy(99L));
        assertFalse(available.canBeRequestedBy(AUTHOR_ID));
        assertFalse(withStatus(PostStatus.RESERVED).canBeRequestedBy(99L));
        assertFalse(withStatus(PostStatus.COMPLETED).canBeRequestedBy(99L));
    }

    @Test
    @DisplayName("작성자인지 판단한다")
    void isWrittenBy() {
        Post post = withStatus(PostStatus.AVAILABLE);

        assertTrue(post.isWrittenBy(AUTHOR_ID));
        assertFalse(post.isWrittenBy(99L));
        assertFalse(post.isWrittenBy(null));
    }

    @Test
    @DisplayName("다른 회원이 올린 사진이면 몇 번째인지 담아 POST_005")
    void create_otherMembersImage_throws() {
        // when
        InvalidImageKeyException e = assertThrows(InvalidImageKeyException.class,
                () -> create(List.of("post/tmp/3/8f14e45f.jpg", "post/tmp/7/c9f0f895.jpg"), null));

        // then
        assertEquals("POST_005", e.getErrorCode().getCode());
        assertEquals("2번째 이미지는 본인이 올린 이미지가 아닙니다. key=post/tmp/7/c9f0f895.jpg", e.getMessage());
    }

    @Test
    @DisplayName("tmp 경로 아래 폴더가 더 있으면 POST_005")
    void create_nestedPath_throws() {
        assertThrows(InvalidImageKeyException.class,
                () -> create(List.of("post/tmp/3/a/8f14e45f.jpg"), null));
    }

    @Test
    @DisplayName("저장 후 사진 key를 글 경로로 옮기고 순서는 유지한다")
    void moveImagesToPost_changesPrefix() {
        // given
        Post saved = Post.builder()
                .id(10L)
                .authorId(AUTHOR_ID)
                .status(PostStatus.AVAILABLE)
                .imageKeys(List.of("post/tmp/3/8f14e45f.jpg", "post/tmp/3/c9f0f895.png"))
                .place(PLACE)
                .build();

        // when
        saved.moveImagesToPost();

        // then
        assertEquals(List.of("post/10/8f14e45f.jpg", "post/10/c9f0f895.png"), saved.getImageKeys());
    }

    @Test
    @DisplayName("저장 전에는 사진을 옮길 수 없다")
    void moveImagesToPost_beforeSave_throws() {
        // given
        Post post = create(List.of("post/tmp/3/8f14e45f.jpg"), null);

        // when · then
        assertThrows(IllegalStateException.class, post::moveImagesToPost);
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

    private static Post withStatus(PostStatus status) {
        return Post.builder()
                .id(10L)
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/10/8f14e45f.jpg"))
                .place(PLACE)
                .build();
    }

    private static Post create(List<String> imageKeys, Integer nowExchangeMinutes) {
        return create(imageKeys, null, nowExchangeMinutes);
    }

    private static Post create(List<String> imageKeys, List<String> tags, Integer nowExchangeMinutes) {
        return Post.create(AUTHOR_ID, imageKeys, "치이카와 하치와레 가챠 키링", PostCategory.CHARACTER, false,
                tags, "치이카와 가챠 3탄에서 뽑았어요.", null, PLACE, nowExchangeMinutes, NOW);
    }
}