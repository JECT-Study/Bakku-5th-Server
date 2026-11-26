package com.ject.post.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.post.PostUpdated;
import com.ject.post.application.FakeMemberRestrictionPort;
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
import com.ject.post.domain.exception.RestrictedMemberException;
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

class UpdatePostHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-11-02T06:30:00Z");
    private static final Long AUTHOR_ID = 3L;

    private FakePostRepository postRepository;
    private FakeMemberRestrictionPort memberRestrictionPort;
    private List<Object> events;
    private UpdatePostHandler handler;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        memberRestrictionPort = new FakeMemberRestrictionPort();
        events = new ArrayList<>();
        handler = new UpdatePostHandler(postRepository, new FakePostImagePort(), memberRestrictionPort, events::add,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("이용 제한 회원이면 글 상태와 상관없이 먼저 POST_006이고, 바꾸거나 발행하지 않는다")
    void handle_restrictedMember_throws() {
        Long postId = save(PostStatus.AVAILABLE);
        memberRestrictionPort.restrict(AUTHOR_ID).restrict(99L);

        assertThrows(RestrictedMemberException.class,
                () -> handler.handle(command(postId, AUTHOR_ID, List.of("post/1/old.jpg"))));
        assertThrows(RestrictedMemberException.class,
                () -> handler.handle(command(404L, 99L, List.of("post/1/old.jpg"))));
        assertEquals("치이카와 하치와레 가챠 키링", postRepository.findById(postId).orElseThrow().getTitle());
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("내용이 바뀌면 저장 후 PostUpdated를 한 번 발행한다 (제목은 수정 후 값)")
    void handle_contentChanged_publishes() {
        Long postId = save(PostStatus.AVAILABLE);

        handler.handle(command(postId, AUTHOR_ID, List.of("post/1/old.jpg")));

        assertEquals(List.of(new PostUpdated(postId, AUTHOR_ID, "새 제품명", NOW)), events);
    }

    @Test
    @DisplayName("사진만 바뀌어도 발행한다 (새 사진 · 순서 변경)")
    void handle_onlyImagesChanged_publishes() {
        Long postId = save(PostStatus.AVAILABLE, List.of("post/1/a.jpg", "post/1/b.jpg"));

        handler.handle(sameContent(postId, List.of("post/1/a.jpg", "post/1/b.jpg", "post/tmp/3/c.jpg")));
        handler.handle(sameContent(postId, List.of("post/1/c.jpg", "post/1/a.jpg", "post/1/b.jpg")));

        assertEquals(2, events.size());
    }

    @Test
    @DisplayName("기존 사진을 그대로 다시 보내고 내용도 같으면 저장 · 발행하지 않는다")
    void handle_nothingChanged_skips() {
        Long postId = save(PostStatus.AVAILABLE, List.of("post/1/a.jpg", "post/1/b.jpg"));
        int savesBefore = postRepository.saveCalls;

        handler.handle(sameContent(postId, List.of("post/1/a.jpg", "post/1/b.jpg")));

        assertTrue(events.isEmpty());
        assertEquals(savesBefore, postRepository.saveCalls);
    }

    @Test
    @DisplayName("실패하면 발행하지 않는다")
    void handle_failure_doesNotPublish() {
        Long postId = save(PostStatus.RESERVED);

        assertThrows(PostReservedException.class,
                () -> handler.handle(command(postId, AUTHOR_ID, List.of("post/1/old.jpg"))));
        assertTrue(events.isEmpty());
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
        return save(status, List.of("post/1/old.jpg"));
    }

    private Long save(PostStatus status, List<String> imageKeys) {
        return postRepository.save(Post.builder()
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(imageKeys)
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

    private static UpdatePostCommand sameContent(Long postId, List<String> imageKeys) {
        return new UpdatePostCommand(postId, AUTHOR_ID, imageKeys, "치이카와 하치와레 가챠 키링", "CHARACTER", false,
                List.of(), null, null,
                new CreatePostCommand.PlaceCommand(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null));
    }
}
