package com.ject.post.domain.domain;

import com.ject.post.domain.exception.NotPostAuthorException;
import com.ject.post.domain.exception.PostCompletedException;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.exception.PostReservedException;
import com.ject.post.domain.exception.PostConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostDeleteTest {

    private static final Long AUTHOR_ID = 3L;
    private static final Instant NOW = Instant.parse("2026-11-23T06:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-11-20T01:00:00Z");

    @Test
    @DisplayName("작성자가 교환 가능 글을 삭제하면 DELETED와 삭제 시각이 남고 true를 돌려준다")
    void delete_available_marksDeleted() {
        // given
        Post post = post(PostStatus.AVAILABLE, null);

        // when
        boolean deleted = post.delete(AUTHOR_ID, NOW);

        // then
        assertTrue(deleted);
        assertEquals(PostStatus.DELETED, post.getStatus());
        assertEquals(NOW, post.getDeletedAt());
    }

    @Test
    @DisplayName("이미 삭제한 내 글을 다시 삭제하면 아무것도 바꾸지 않고 false를 돌려준다")
    void delete_alreadyDeletedByAuthor_noChange() {
        // given
        Post post = post(PostStatus.DELETED, DELETED_AT);

        // when
        boolean deleted = post.delete(AUTHOR_ID, NOW);

        // then
        assertFalse(deleted);
        assertEquals(PostStatus.DELETED, post.getStatus());
        assertEquals(DELETED_AT, post.getDeletedAt());
    }

    @Test
    @DisplayName("남의 삭제된 글이면 POST_001")
    void delete_deletedByOther_throwsNotFound() {
        Post post = post(PostStatus.DELETED, DELETED_AT);

        PostNotFoundException e = assertThrows(PostNotFoundException.class, () -> post.delete(99L, NOW));

        assertEquals("POST_001", e.getErrorCode().getCode());
    }

    @Test
    @DisplayName("작성자가 아니면 POST_002, 삭제용 문구")
    void delete_notAuthor_throws() {
        Post post = post(PostStatus.AVAILABLE, null);

        NotPostAuthorException e = assertThrows(NotPostAuthorException.class, () -> post.delete(99L, NOW));

        assertEquals("POST_002", e.getErrorCode().getCode());
        assertEquals("작성자만 삭제할 수 있습니다. id=10", e.getMessage());
    }

    @Test
    @DisplayName("작성자가 아니면 글 상태보다 먼저 POST_002")
    void delete_notAuthorOnReserved_throwsNotAuthor() {
        Post post = post(PostStatus.RESERVED, null);

        assertThrows(NotPostAuthorException.class, () -> post.delete(99L, NOW));
    }

    @Test
    @DisplayName("예약중이면 POST_003, 삭제용 문구")
    void delete_reserved_throws() {
        Post post = post(PostStatus.RESERVED, null);

        PostReservedException e = assertThrows(PostReservedException.class, () -> post.delete(AUTHOR_ID, NOW));

        assertEquals("POST_003", e.getErrorCode().getCode());
        assertEquals("약속이 잡힌 글은 삭제할 수 없습니다. id=10", e.getMessage());
        assertEquals(PostStatus.RESERVED, post.getStatus());
    }

    @Test
    @DisplayName("교환 완료면 POST_004, 삭제용 문구")
    void delete_completed_throws() {
        Post post = post(PostStatus.COMPLETED, null);

        PostCompletedException e = assertThrows(PostCompletedException.class, () -> post.delete(AUTHOR_ID, NOW));

        assertEquals("POST_004", e.getErrorCode().getCode());
        assertEquals("교환이 완료된 글은 삭제할 수 없습니다. id=10", e.getMessage());
    }

    @Test
    @DisplayName("동시 변경 충돌은 POST_007, 삭제용 문구")
    void conflictOnDelete_message() {
        PostConflictException e = PostConflictException.onDelete(10L);

        assertEquals("POST_007", e.getErrorCode().getCode());
        assertEquals("다른 변경이 먼저 반영되어 삭제할 수 없습니다. id=10", e.getMessage());
    }

    @Test
    @DisplayName("작성자 탈퇴: 교환 가능 · 예약중 글은 DELETED와 탈퇴 시각이 남고 true")
    void deleteByAuthorWithdrawal_availableOrReserved_marksDeleted() {
        Post available = post(PostStatus.AVAILABLE, null);
        Post reserved = post(PostStatus.RESERVED, null);

        assertTrue(available.deleteByAuthorWithdrawal(NOW));
        assertTrue(reserved.deleteByAuthorWithdrawal(NOW));

        assertEquals(PostStatus.DELETED, available.getStatus());
        assertEquals(PostStatus.DELETED, reserved.getStatus());
        assertEquals(NOW, reserved.getDeletedAt());
    }

    @Test
    @DisplayName("작성자 탈퇴: 교환 완료 · 이미 삭제한 글은 그대로 두고 false")
    void deleteByAuthorWithdrawal_completedOrDeleted_keeps() {
        Post completed = post(PostStatus.COMPLETED, null);
        Post deleted = post(PostStatus.DELETED, DELETED_AT);

        assertFalse(completed.deleteByAuthorWithdrawal(NOW));
        assertFalse(deleted.deleteByAuthorWithdrawal(NOW));

        assertEquals(PostStatus.COMPLETED, completed.getStatus());
        assertEquals(DELETED_AT, deleted.getDeletedAt());
    }

    private static Post post(PostStatus status, Instant deletedAt) {
        return Post.builder()
                .id(10L)
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/10/a.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .opened(false)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null))
                .deletedAt(deletedAt)
                .build();
    }
}
