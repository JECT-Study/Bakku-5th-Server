package com.ject.post.domain.domain;

import com.ject.post.domain.exception.NotPostAuthorException;
import com.ject.post.domain.exception.PostCompletedException;
import com.ject.post.domain.exception.PostReservedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostUpdateTest {

    private static final Long AUTHOR_ID = 3L;
    private static final Instant ENDS_AT = Instant.parse("2026-11-02T06:30:00Z");
    private static final Place NEW_PLACE = new Place(37.5444, 127.0557, "성수역 4번 출구", "서울특별시 성동구 성수동2가", null);

    @Test
    @DisplayName("작성자가 교환 가능 글을 수정하면 내용 전체가 바뀌고, 지금 교환 종료 시각은 그대로다")
    void update_replacesContent() {
        // given
        Post post = post(PostStatus.AVAILABLE);

        // when
        post.update(AUTHOR_ID, List.of("post/10/a.jpg", "post/tmp/3/b.jpg"), "새 제품명", PostCategory.IDOL, true,
                List.of(" Chii Kawa ", "chiikawa"), null, null, NEW_PLACE);

        // then
        assertEquals(List.of("post/10/a.jpg", "post/tmp/3/b.jpg"), post.getImageKeys());
        assertEquals("새 제품명", post.getTitle());
        assertEquals(PostCategory.IDOL, post.getCategory());
        assertEquals(true, post.isOpened());
        assertEquals(List.of("chiikawa"), post.getTags());
        assertNull(post.getDescription());
        assertNull(post.getWantedItem());
        assertEquals(NEW_PLACE, post.getPlace());
        assertEquals(ENDS_AT, post.getNowExchangeEndsAt());
        assertEquals(PostStatus.AVAILABLE, post.getStatus());
    }

    @Test
    @DisplayName("작성자가 아니면 POST_002")
    void update_notAuthor_throws() {
        Post post = post(PostStatus.AVAILABLE);

        NotPostAuthorException e = assertThrows(NotPostAuthorException.class, () -> update(post, 99L));

        assertEquals("POST_002", e.getErrorCode().getCode());
    }

    @Test
    @DisplayName("작성자가 아니면 글 상태보다 먼저 POST_002")
    void update_notAuthorOnReserved_throwsNotAuthor() {
        Post post = post(PostStatus.RESERVED);

        assertThrows(NotPostAuthorException.class, () -> update(post, 99L));
    }

    @Test
    @DisplayName("예약중이면 POST_003")
    void update_reserved_throws() {
        Post post = post(PostStatus.RESERVED);

        PostReservedException e = assertThrows(PostReservedException.class, () -> update(post, AUTHOR_ID));

        assertEquals("POST_003", e.getErrorCode().getCode());
    }

    @Test
    @DisplayName("교환 완료면 POST_004")
    void update_completed_throws() {
        Post post = post(PostStatus.COMPLETED);

        PostCompletedException e = assertThrows(PostCompletedException.class, () -> update(post, AUTHOR_ID));

        assertEquals("POST_004", e.getErrorCode().getCode());
    }

    private static void update(Post post, Long editorId) {
        post.update(editorId, List.of("post/10/a.jpg"), "새 제품명", PostCategory.IDOL, true,
                List.of(), null, null, NEW_PLACE);
    }

    private static Post post(PostStatus status) {
        return Post.builder()
                .id(10L)
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/10/a.jpg"))
                .tags(List.of("하치와레"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .opened(false)
                .description("설명")
                .wantedItem("치이카와 가챠 키링")
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", "AK플라자 주변"))
                .nowExchangeEndsAt(ENDS_AT)
                .build();
    }
}
