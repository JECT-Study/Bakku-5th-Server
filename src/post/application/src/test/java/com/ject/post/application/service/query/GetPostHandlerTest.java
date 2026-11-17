package com.ject.post.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.FakePostRepository;
import com.ject.post.application.dto.AuthorInfo;
import com.ject.post.application.dto.PostResult;
import com.ject.post.application.dto.query.GetPostQuery;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.PostNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPostHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");
    private static final Long AUTHOR_ID = 3L;
    private static final AuthorInfo AUTHOR = new AuthorInfo(AUTHOR_ID, "하치와레러버", 12);

    private FakePostRepository postRepository;
    private GetPostHandler handler;

    @BeforeEach
    void setUp() {
        postRepository = new FakePostRepository(NOW);
        ServiceTime serviceTime = new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        handler = new GetPostHandler(postRepository, ids -> Map.of(AUTHOR_ID, AUTHOR), serviceTime);
    }

    @Test
    @DisplayName("글과 작성자를 합쳐 돌려주고, 지역은 구+동, 시각은 +09:00이다")
    void handle_returnsResult() {
        // given
        Long postId = save(PostStatus.AVAILABLE, 60).getId();

        // when
        PostResult result = handler.handle(new GetPostQuery(postId, AUTHOR_ID));

        // then
        assertEquals("AVAILABLE", result.status());
        assertEquals("CHARACTER", result.category());
        assertEquals("마포구 동교동", result.place().regionName());
        assertEquals(AUTHOR, result.author());
        assertTrue(result.isMine());
        assertTrue(result.nowExchange());
        assertEquals(OffsetDateTime.parse("2026-11-02T15:30:00+09:00"), result.nowExchangeEndsAt());
        assertEquals(OffsetDateTime.parse("2026-11-02T14:30:00+09:00"), result.createdAt());
        assertEquals(0, result.requestCount());
        assertNull(result.chatRoomId());
    }

    @Test
    @DisplayName("다른 사람이 보면 내 글이 아니다")
    void handle_otherViewer_isNotMine() {
        // given
        Long postId = save(PostStatus.AVAILABLE, null).getId();

        // when
        PostResult result = handler.handle(new GetPostQuery(postId, 99L));

        // then
        assertFalse(result.isMine());
        assertFalse(result.nowExchange());
        assertNull(result.nowExchangeEndsAt());
    }

    @Test
    @DisplayName("삭제된 글은 POST_001")
    void handle_deletedPost_throws() {
        // given
        Long postId = save(PostStatus.DELETED, null).getId();

        // when · then
        assertThrows(PostNotFoundException.class, () -> handler.handle(new GetPostQuery(postId, AUTHOR_ID)));
    }

    @Test
    @DisplayName("없는 글은 POST_001")
    void handle_missingPost_throws() {
        assertThrows(PostNotFoundException.class, () -> handler.handle(new GetPostQuery(404L, AUTHOR_ID)));
    }

    private Post save(PostStatus status, Integer nowExchangeMinutes) {
        return postRepository.save(Post.builder()
                .authorId(AUTHOR_ID)
                .status(status)
                .imageKeys(List.of("post/1/8f14e45f.jpg"))
                .title("치이카와 하치와레 가챠 키링")
                .category(PostCategory.CHARACTER)
                .place(new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", "AK플라자 주변"))
                .nowExchangeEndsAt(nowExchangeMinutes == null ? null : NOW.plus(Duration.ofMinutes(nowExchangeMinutes)))
                .build());
    }
}
