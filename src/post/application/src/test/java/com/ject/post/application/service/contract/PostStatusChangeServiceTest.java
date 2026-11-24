package com.ject.post.application.service.contract;

import com.ject.post.application.FakePostRepository;
import com.ject.post.application.contract.PostStatusChangeRejectedException;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.PostConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostStatusChangeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T06:00:00Z");

    private FakePostRepository postRepository;
    private PostStatusChangeService service;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        service = new PostStatusChangeService(postRepository);
    }

    @Test
    @DisplayName("예약: 게시글과 첨부 글이 모두 예약중이 된다")
    void reserve_all() {
        Long first = save(PostStatus.AVAILABLE);
        Long second = save(PostStatus.AVAILABLE);

        service.reserve(List.of(first, second));

        assertEquals(PostStatus.RESERVED, statusOf(first));
        assertEquals(PostStatus.RESERVED, statusOf(second));
    }

    @Test
    @DisplayName("해제: 예약중 → 교환 가능")
    void release_all() {
        Long postId = save(PostStatus.RESERVED);

        service.release(List.of(postId));

        assertEquals(PostStatus.AVAILABLE, statusOf(postId));
    }

    @Test
    @DisplayName("완료: 예약중 → 교환 완료")
    void complete_all() {
        Long postId = save(PostStatus.RESERVED);

        service.complete(List.of(postId));

        assertEquals(PostStatus.COMPLETED, statusOf(postId));
    }

    @Test
    @DisplayName("하나라도 상태가 안 맞으면 INVALID_STATUS로 거절하고 아무것도 저장하지 않는다")
    void reserve_oneInvalid_rejectsAll() {
        Long available = save(PostStatus.AVAILABLE);
        Long reserved = save(PostStatus.RESERVED);

        PostStatusChangeRejectedException e = assertThrows(PostStatusChangeRejectedException.class,
                () -> service.reserve(List.of(available, reserved)));

        assertEquals(reserved, e.getPostId());
        assertEquals(PostStatusChangeRejectedException.Reason.INVALID_STATUS, e.getReason());
        assertEquals(0, postRepository.updateStatusesCalls);
        assertEquals(PostStatus.AVAILABLE, statusOf(available));
    }

    @Test
    @DisplayName("삭제된 글이면 NOT_FOUND로 거절한다")
    void reserve_deleted_rejectsNotFound() {
        Long deleted = save(PostStatus.DELETED);

        PostStatusChangeRejectedException e = assertThrows(PostStatusChangeRejectedException.class,
                () -> service.reserve(List.of(deleted)));

        assertEquals(PostStatusChangeRejectedException.Reason.NOT_FOUND, e.getReason());
    }

    @Test
    @DisplayName("없는 글이면 NOT_FOUND로 거절한다")
    void reserve_missing_rejectsNotFound() {
        PostStatusChangeRejectedException e = assertThrows(PostStatusChangeRejectedException.class,
                () -> service.reserve(List.of(404L)));

        assertEquals(404L, e.getPostId());
        assertEquals(PostStatusChangeRejectedException.Reason.NOT_FOUND, e.getReason());
    }

    @Test
    @DisplayName("저장 중 동시 변경 충돌이면 CONFLICT로 거절한다")
    void reserve_conflict_rejectsConflict() {
        Long postId = save(PostStatus.AVAILABLE);
        postRepository.updateStatusesFailure = PostConflictException.onStatusChange(postId);

        PostStatusChangeRejectedException e = assertThrows(PostStatusChangeRejectedException.class,
                () -> service.reserve(List.of(postId)));

        assertEquals(postId, e.getPostId());
        assertEquals(PostStatusChangeRejectedException.Reason.CONFLICT, e.getReason());
    }

    @Test
    @DisplayName("중복 id는 한 번만 바꾼다")
    void reserve_duplicateIds_once() {
        Long postId = save(PostStatus.AVAILABLE);

        service.reserve(List.of(postId, postId));

        assertEquals(PostStatus.RESERVED, statusOf(postId));
    }

    @Test
    @DisplayName("빈 목록이면 아무것도 하지 않는다")
    void reserve_empty_noop() {
        service.reserve(List.of());

        assertEquals(0, postRepository.updateStatusesCalls);
    }

    private PostStatus statusOf(Long postId) {
        return postRepository.findById(postId).orElseThrow().getStatus();
    }

    private Long save(PostStatus status) {
        return postRepository.save(Post.builder()
                .authorId(3L)
                .status(status)
                .imageKeys(List.of("post/1/a.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .opened(false)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null))
                .deletedAt(status == PostStatus.DELETED ? NOW : null)
                .build()).getId();
    }
}
