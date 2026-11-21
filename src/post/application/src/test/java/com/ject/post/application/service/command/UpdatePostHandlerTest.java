package com.ject.post.application.service.command;

import com.ject.post.application.FakePostImagePort;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.application.dto.command.UpdatePostCommand;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.InvalidImageKeyException;
import com.ject.post.domain.exception.NotPostAuthorException;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.exception.PostReservedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdatePostHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-11-02T06:30:00Z");
    private static final Long AUTHOR_ID = 3L;

    private FakePostRepository postRepository;
    private UpdatePostHandler handler;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        handler = new UpdatePostHandler(postRepository, new FakePostImagePort());
    }

    @Test
    @DisplayName("기존 사진은 그대로, 새 사진은 글 경로로 옮겨 순서대로 저장하고, 지금 교환 종료 시각은 그대로다")
    void handle_updatesPost() {
        // given
        Long postId = save(PostStatus.AVAILABLE);

        // when
        handler.handle(command(postId, AUTHOR_ID, List.of("post/tmp/3/new.png", "post/1/old.jpg")));

        // then
        Post updated = postRepository.findById(postId).orElseThrow();
        assertEquals(List.of("post/1/new.png", "post/1/old.jpg"), updated.getImageKeys());
        assertEquals("새 제품명", updated.getTitle());
        assertEquals(PostCategory.IDOL, updated.getCategory());
        assertEquals(List.of("치이카와"), updated.getTags());
        assertEquals("성수역 4번 출구", updated.getPlace().name());
        assertEquals(ENDS_AT, updated.getNowExchangeEndsAt());
    }

    @Test
    @DisplayName("작성자가 아니면 POST_002")
    void handle_notAuthor_throws() {
        Long postId = save(PostStatus.AVAILABLE);

        assertThrows(NotPostAuthorException.class,
                () -> handler.handle(command(postId, 99L, List.of("post/1/old.jpg"))));
    }

    @Test
    @DisplayName("예약중이면 POST_003")
    void handle_reserved_throws() {
        Long postId = save(PostStatus.RESERVED);

        assertThrows(PostReservedException.class,
                () -> handler.handle(command(postId, AUTHOR_ID, List.of("post/1/old.jpg"))));
    }

    @Test
    @DisplayName("삭제된 글 · 없는 글은 POST_001")
    void handle_deletedOrMissing_throws() {
        Long deletedId = save(PostStatus.DELETED);

        assertThrows(PostNotFoundException.class,
                () -> handler.handle(command(deletedId, AUTHOR_ID, List.of("post/1/old.jpg"))));
        assertThrows(PostNotFoundException.class,
                () -> handler.handle(command(404L, AUTHOR_ID, List.of("post/1/old.jpg"))));
    }

    @Test
    @DisplayName("이미지 모듈이 사진을 거절하면 POST_005")
    void handle_rejectedImage_throws() {
        Long postId = save(PostStatus.AVAILABLE);

        assertThrows(InvalidImageKeyException.class,
                () -> handler.handle(command(postId, AUTHOR_ID, List.of("post/tmp/7/other.jpg"))));
    }

    private Long save(PostStatus status) {
        return postRepository.save(Post.builder()
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/1/old.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null))
                .nowExchangeEndsAt(ENDS_AT)
                .build()).getId();
    }

    private static UpdatePostCommand command(Long postId, Long editorId, List<String> imageKeys) {
        return new UpdatePostCommand(postId, editorId, imageKeys, "새 제품명", "IDOL", true,
                List.of("치이 카와"), null, null,
                new CreatePostCommand.PlaceCommand(37.54482, 127.05481, "성수역 4번 출구", "서울특별시 성동구 성수동2가", null));
    }
}
