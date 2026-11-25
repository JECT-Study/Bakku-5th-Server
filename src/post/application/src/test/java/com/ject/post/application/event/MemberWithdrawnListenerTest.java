package com.ject.post.application.event;

import com.ject.common.event.member.MemberWithdrawn;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.service.command.WithdrawnMemberPostService;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberWithdrawnListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-25T03:00:00Z");
    private static final Long WITHDRAWN_ID = 3L;
    private static final Long OTHER_ID = 4L;

    @Test
    @DisplayName("탈퇴하면 그 회원의 교환 가능 글은 탈퇴 시각으로 삭제되고, 예약중 글 · 다른 회원 글은 그대로다")
    void handle_deletesAvailablePosts() {
        // given
        FakePostRepository postRepository = new FakePostRepository(NOW);
        Long available = save(postRepository, WITHDRAWN_ID, PostStatus.AVAILABLE);
        Long reserved = save(postRepository, WITHDRAWN_ID, PostStatus.RESERVED);
        Long other = save(postRepository, OTHER_ID, PostStatus.AVAILABLE);

        // when
        listener(postRepository).handle(new MemberWithdrawn(WITHDRAWN_ID, NOW));

        // then
        Post deleted = postRepository.findById(available).orElseThrow();
        assertEquals(PostStatus.DELETED, deleted.getStatus());
        assertEquals(NOW, deleted.getDeletedAt());
        assertEquals(PostStatus.RESERVED, postRepository.findById(reserved).orElseThrow().getStatus());
        assertEquals(PostStatus.AVAILABLE, postRepository.findById(other).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("삭제에 실패해도 예외를 넘기지 않는다 (탈퇴는 이미 커밋됨)")
    void handle_failure_doesNotThrow() {
        // given
        FakePostRepository postRepository = new FakePostRepository(NOW) {
            @Override
            public int deleteAvailableByAuthor(Long authorId, Instant deletedAt) {
                throw new IllegalStateException("DB 오류");
            }
        };

        // when · then
        assertDoesNotThrow(() -> listener(postRepository).handle(new MemberWithdrawn(WITHDRAWN_ID, NOW)));
    }

    private static MemberWithdrawnListener listener(FakePostRepository postRepository) {
        return new MemberWithdrawnListener(new WithdrawnMemberPostService(postRepository));
    }

    private static Long save(FakePostRepository postRepository, Long authorId, PostStatus status) {
        return postRepository.save(Post.builder()
                .authorId(authorId)
                .status(status)
                .imageKeys(List.of("post/1/8f14e45f.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null))
                .build()).getId();
    }
}
