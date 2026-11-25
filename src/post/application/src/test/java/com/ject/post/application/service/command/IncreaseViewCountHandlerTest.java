package com.ject.post.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.FakeBlockRelationPort;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.FakePostViewLog;
import com.ject.post.application.dto.command.IncreaseViewCountCommand;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.PostNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncreaseViewCountHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Long AUTHOR_ID = 3L;
    private static final Long VIEWER_ID = 99L;

    private FakePostRepository postRepository;
    private FakePostViewLog postViewLog;
    private FakeBlockRelationPort blockRelationPort;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        postViewLog = new FakePostViewLog();
        blockRelationPort = new FakeBlockRelationPort();
    }

    @Test
    @DisplayName("차단 관계면 POST_001이고 조회 수는 오르지 않는다")
    void handle_blocked_throwsAndDoesNotIncrease() {
        // given
        Long postId = save(PostStatus.AVAILABLE);
        blockRelationPort.block(VIEWER_ID, AUTHOR_ID);

        // when · then
        assertThrows(PostNotFoundException.class,
                () -> handlerAt(NOW).handle(new IncreaseViewCountCommand(postId, VIEWER_ID)));
        assertEquals(0, viewCount(postId));
    }

    @Test
    @DisplayName("다른 사람 글을 처음 열면 조회 수가 1 오른다")
    void handle_firstView_increases() {
        // given
        Long postId = save(PostStatus.AVAILABLE);

        // when
        handlerAt(NOW).handle(new IncreaseViewCountCommand(postId, VIEWER_ID));

        // then
        assertEquals(1, viewCount(postId));
    }

    @Test
    @DisplayName("같은 날 다시 열면 오르지 않는다")
    void handle_sameDay_increasesOnce() {
        // given
        Long postId = save(PostStatus.AVAILABLE);
        IncreaseViewCountHandler handler = handlerAt(NOW);

        // when
        handler.handle(new IncreaseViewCountCommand(postId, VIEWER_ID));
        handler.handle(new IncreaseViewCountCommand(postId, VIEWER_ID));

        // then
        assertEquals(1, viewCount(postId));
    }

    @Test
    @DisplayName("한국 시간 자정이 지나 다시 열면 또 오른다")
    void handle_nextDayInSeoul_increasesAgain() {
        // given
        Long postId = save(PostStatus.AVAILABLE);
        Instant beforeMidnight = Instant.parse("2026-11-02T14:59:00Z");
        Instant afterMidnight = Instant.parse("2026-11-02T15:01:00Z");

        // when
        handlerAt(beforeMidnight).handle(new IncreaseViewCountCommand(postId, VIEWER_ID));
        handlerAt(afterMidnight).handle(new IncreaseViewCountCommand(postId, VIEWER_ID));

        // then
        assertEquals(2, viewCount(postId));
    }

    @Test
    @DisplayName("다른 회원이 열면 각각 오른다")
    void handle_otherViewers_increaseEach() {
        // given
        Long postId = save(PostStatus.AVAILABLE);
        IncreaseViewCountHandler handler = handlerAt(NOW);

        // when
        handler.handle(new IncreaseViewCountCommand(postId, VIEWER_ID));
        handler.handle(new IncreaseViewCountCommand(postId, 100L));

        // then
        assertEquals(2, viewCount(postId));
    }

    @Test
    @DisplayName("내 글은 열어도 오르지 않는다")
    void handle_myPost_doesNotIncrease() {
        // given
        Long postId = save(PostStatus.AVAILABLE);

        // when
        handlerAt(NOW).handle(new IncreaseViewCountCommand(postId, AUTHOR_ID));

        // then
        assertEquals(0, viewCount(postId));
    }

    @Test
    @DisplayName("예약중 · 교환 완료 글도 센다")
    void handle_reservedPost_increases() {
        // given
        Long postId = save(PostStatus.RESERVED);

        // when
        handlerAt(NOW).handle(new IncreaseViewCountCommand(postId, VIEWER_ID));

        // then
        assertEquals(1, viewCount(postId));
    }

    @Test
    @DisplayName("삭제된 글 · 없는 글은 POST_001")
    void handle_deletedOrMissing_throws() {
        // given
        Long deletedId = save(PostStatus.DELETED);
        IncreaseViewCountHandler handler = handlerAt(NOW);

        // when · then
        assertThrows(PostNotFoundException.class,
                () -> handler.handle(new IncreaseViewCountCommand(deletedId, VIEWER_ID)));
        assertThrows(PostNotFoundException.class,
                () -> handler.handle(new IncreaseViewCountCommand(404L, VIEWER_ID)));
    }

    private IncreaseViewCountHandler handlerAt(Instant now) {
        return new IncreaseViewCountHandler(postRepository, postViewLog, blockRelationPort,
                new ServiceTime(Clock.fixed(now, SEOUL)));
    }

    private int viewCount(Long postId) {
        return postRepository.findById(postId).orElseThrow().getViewCount();
    }

    private Long save(PostStatus status) {
        return postRepository.save(Post.builder()
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/1/8f14e45f.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", "AK플라자 주변"))
                .build()).getId();
    }
}
