package com.ject.post.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.post.PostDeleted;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.dto.command.DeletePostCommand;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.NotPostAuthorException;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.exception.PostReservedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeletePostHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-23T06:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-11-20T01:00:00Z");
    private static final Long AUTHOR_ID = 3L;

    private FakePostRepository postRepository;
    private List<Object> events;
    private DeletePostHandler handler;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        events = new ArrayList<>();
        handler = new DeletePostHandler(postRepository, events::add,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("작성자가 삭제하면 DELETED · 삭제 시각을 저장하고 PostDeleted를 한 번 발행한다")
    void handle_deletes_publishesEvent() {
        // given
        Long postId = save(PostStatus.AVAILABLE, null);

        // when
        handler.handle(new DeletePostCommand(postId, AUTHOR_ID));

        // then
        Post deleted = postRepository.findById(postId).orElseThrow();
        assertEquals(PostStatus.DELETED, deleted.getStatus());
        assertEquals(NOW, deleted.getDeletedAt());
        assertEquals(1, postRepository.deleteCalls);
        assertEquals(List.of(new PostDeleted(postId, AUTHOR_ID, NOW)), events);
    }

    @Test
    @DisplayName("이미 삭제한 내 글을 다시 삭제하면 성공하고, 저장 · 이벤트는 없다")
    void handle_alreadyDeleted_noSaveNoEvent() {
        // given
        Long postId = save(PostStatus.DELETED, DELETED_AT);

        // when
        handler.handle(new DeletePostCommand(postId, AUTHOR_ID));

        // then
        assertEquals(DELETED_AT, postRepository.findById(postId).orElseThrow().getDeletedAt());
        assertEquals(0, postRepository.deleteCalls);
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("남의 삭제된 글이면 POST_001")
    void handle_deletedByOther_throwsNotFound() {
        Long postId = save(PostStatus.DELETED, DELETED_AT);

        assertThrows(PostNotFoundException.class, () -> handler.handle(new DeletePostCommand(postId, 99L)));
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("없는 글이면 POST_001")
    void handle_missing_throwsNotFound() {
        assertThrows(PostNotFoundException.class, () -> handler.handle(new DeletePostCommand(404L, AUTHOR_ID)));
    }

    @Test
    @DisplayName("작성자가 아니면 POST_002, 이벤트 없음")
    void handle_notAuthor_throws() {
        Long postId = save(PostStatus.AVAILABLE, null);

        assertThrows(NotPostAuthorException.class, () -> handler.handle(new DeletePostCommand(postId, 99L)));
        assertEquals(0, postRepository.deleteCalls);
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("예약중이면 POST_003, 이벤트 없음")
    void handle_reserved_throws() {
        Long postId = save(PostStatus.RESERVED, null);

        assertThrows(PostReservedException.class, () -> handler.handle(new DeletePostCommand(postId, AUTHOR_ID)));
        assertTrue(events.isEmpty());
    }

    private Long save(PostStatus status, Instant deletedAt) {
        return postRepository.save(Post.builder()
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/1/a.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .opened(false)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null))
                .deletedAt(deletedAt)
                .build()).getId();
    }
}
