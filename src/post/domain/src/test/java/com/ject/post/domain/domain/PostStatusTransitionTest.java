package com.ject.post.domain.domain;

import com.ject.post.domain.exception.PostConflictException;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.exception.PostStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostStatusTransitionTest {

    @Test
    @DisplayName("예약: 교환 가능 → 예약중")
    void reserve_available() {
        Post post = post(PostStatus.AVAILABLE);

        post.reserve();

        assertEquals(PostStatus.RESERVED, post.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = PostStatus.class, names = {"RESERVED", "COMPLETED"})
    @DisplayName("예약: 교환 가능이 아니면 POST_008, 상태는 그대로")
    void reserve_notAvailable_throws(PostStatus status) {
        Post post = post(status);

        PostStatusTransitionException e = assertThrows(PostStatusTransitionException.class, post::reserve);

        assertEquals("POST_008", e.getErrorCode().getCode());
        assertEquals(status, post.getStatus());
    }

    @Test
    @DisplayName("해제: 예약중 → 교환 가능")
    void release_reserved() {
        Post post = post(PostStatus.RESERVED);

        post.release();

        assertEquals(PostStatus.AVAILABLE, post.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = PostStatus.class, names = {"AVAILABLE", "COMPLETED"})
    @DisplayName("해제: 예약중이 아니면 POST_008")
    void release_notReserved_throws(PostStatus status) {
        assertThrows(PostStatusTransitionException.class, post(status)::release);
    }

    @Test
    @DisplayName("완료: 예약중 → 교환 완료")
    void complete_reserved() {
        Post post = post(PostStatus.RESERVED);

        post.complete();

        assertEquals(PostStatus.COMPLETED, post.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = PostStatus.class, names = {"AVAILABLE", "COMPLETED"})
    @DisplayName("완료: 예약중이 아니면 POST_008")
    void complete_notReserved_throws(PostStatus status) {
        assertThrows(PostStatusTransitionException.class, post(status)::complete);
    }

    @Test
    @DisplayName("삭제된 글은 예약 · 해제 · 완료 모두 POST_001")
    void deleted_throwsNotFound() {
        Post post = post(PostStatus.DELETED);

        assertThrows(PostNotFoundException.class, post::reserve);
        assertThrows(PostNotFoundException.class, post::release);
        assertThrows(PostNotFoundException.class, post::complete);
    }

    @Test
    @DisplayName("상태가 안 맞으면 메시지에 글 id가 들어간다")
    void transition_message() {
        PostStatusTransitionException e = assertThrows(PostStatusTransitionException.class,
                post(PostStatus.COMPLETED)::reserve);

        assertEquals("지금 상태에서는 바꿀 수 없는 글입니다. id=10", e.getMessage());
    }

    @Test
    @DisplayName("상태 변경 중 동시 변경 충돌은 POST_007, 상태 변경용 문구")
    void conflictOnStatusChange_message() {
        PostConflictException e = PostConflictException.onStatusChange(10L);

        assertEquals("POST_007", e.getErrorCode().getCode());
        assertEquals("다른 변경이 먼저 반영되어 상태를 변경할 수 없습니다. id=10", e.getMessage());
    }

    private static Post post(PostStatus status) {
        return Post.builder()
                .id(10L)
                .authorId(3L)
                .status(status)
                .imageKeys(List.of("post/10/a.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .opened(false)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null))
                .deletedAt(status == PostStatus.DELETED ? Instant.parse("2026-11-20T01:00:00Z") : null)
                .build();
    }
}
