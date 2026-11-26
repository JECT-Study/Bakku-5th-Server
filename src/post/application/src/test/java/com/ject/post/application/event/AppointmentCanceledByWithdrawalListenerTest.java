package com.ject.post.application.event;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.exchange.AppointmentCanceledByMemberWithdrawal;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.service.command.WithdrawnMemberReservedPostService;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentCanceledByWithdrawalListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T03:00:00Z");
    private static final Long WITHDRAWN_ID = 3L;
    private static final Long COUNTERPART_ID = 4L;

    @Test
    @DisplayName("탈퇴 회원 글은 삭제하고, 상대의 예약중 글은 교환 가능으로 되돌린다")
    void handle_deletesWithdrawnAndReleasesCounterpart() {
        // given
        FakePostRepository postRepository = new FakePostRepository(NOW);
        Long withdrawnPost = save(postRepository, WITHDRAWN_ID, PostStatus.RESERVED);
        Long counterpartPost = save(postRepository, COUNTERPART_ID, PostStatus.RESERVED);

        // when
        listener(postRepository).handle(event(List.of(withdrawnPost, counterpartPost)));

        // then
        Post deleted = postRepository.findById(withdrawnPost).orElseThrow();
        assertEquals(PostStatus.DELETED, deleted.getStatus());
        assertEquals(NOW, deleted.getDeletedAt());
        assertEquals(PostStatus.AVAILABLE, postRepository.findById(counterpartPost).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("교환 완료 · 이미 삭제된 글 · 예약중이 아닌 상대 글 · 없는 글은 건드리지 않는다")
    void handle_skipsOthers() {
        // given
        FakePostRepository postRepository = new FakePostRepository(NOW);
        Long completed = save(postRepository, WITHDRAWN_ID, PostStatus.COMPLETED);
        Long alreadyDeleted = save(postRepository, WITHDRAWN_ID, PostStatus.DELETED);
        Long counterpartAvailable = save(postRepository, COUNTERPART_ID, PostStatus.AVAILABLE);

        // when
        listener(postRepository).handle(event(List.of(completed, alreadyDeleted, counterpartAvailable, 404L)));

        // then
        assertEquals(PostStatus.COMPLETED, postRepository.findById(completed).orElseThrow().getStatus());
        assertEquals(PostStatus.AVAILABLE, postRepository.findById(counterpartAvailable).orElseThrow().getStatus());
        assertEquals(0, postRepository.deleteCalls);
        assertEquals(0, postRepository.updateStatusesCalls);
    }

    @Test
    @DisplayName("처리에 실패해도 예외를 넘기지 않는다 (약속 취소는 이미 커밋됨)")
    void handle_failure_doesNotThrow() {
        // given
        FakePostRepository postRepository = new FakePostRepository(NOW);
        Long counterpartPost = save(postRepository, COUNTERPART_ID, PostStatus.RESERVED);
        postRepository.updateStatusesFailure = new IllegalStateException("DB 오류");

        // when · then
        assertDoesNotThrow(() -> listener(postRepository).handle(event(List.of(counterpartPost))));
    }

    private static AppointmentCanceledByWithdrawalListener listener(FakePostRepository postRepository) {
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        return new AppointmentCanceledByWithdrawalListener(
                new WithdrawnMemberReservedPostService(postRepository, serviceTime));
    }

    private static AppointmentCanceledByMemberWithdrawal event(List<Long> postIds) {
        return new AppointmentCanceledByMemberWithdrawal(10L, postIds, WITHDRAWN_ID);
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
