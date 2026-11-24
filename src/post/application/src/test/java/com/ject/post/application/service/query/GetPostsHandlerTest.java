package com.ject.post.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.FakePostSummaryReader;
import com.ject.post.application.dto.PostPageResult;
import com.ject.post.application.dto.PostSummaryResult;
import com.ject.post.application.dto.query.GetPostsQuery;
import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPostsHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T05:30:00Z");
    private static final Long VIEWER_ID = 3L;

    @Test
    @DisplayName("size + 1개를 요청하고, 더 있으면 size개만 주고 마지막 ID를 다음 커서로 준다")
    void handle_hasNext() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(rows(30, 29, 28));

        // when
        PostPageResult result = handler(reader).handle(new GetPostsQuery(VIEWER_ID, null, null, null, null, 2));

        // then
        assertEquals(3, reader.receivedLimit);
        assertEquals(List.of(30L, 29L), result.content().stream().map(PostSummaryResult::id).toList());
        assertTrue(result.hasNext());
        assertEquals(29L, result.nextCursorId());
    }

    @Test
    @DisplayName("마지막 페이지면 hasNext false, 다음 커서 null")
    void handle_lastPage() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(rows(30, 29));

        // when
        PostPageResult result = handler(reader).handle(new GetPostsQuery(VIEWER_ID, null, null, null, 31L, 2));

        // then
        assertEquals(2, result.content().size());
        assertFalse(result.hasNext());
        assertNull(result.nextCursorId());
        assertEquals(31L, reader.receivedCursorId);
    }

    @Test
    @DisplayName("교환 방식 · 카테고리 · 거점 · 현재 시각을 조건으로 넘긴다")
    void handle_passesCondition() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of());

        // when
        handler(reader).handle(new GetPostsQuery(VIEWER_ID, true, "IDOL", "SINDORIM", null, 20));

        // then
        assertEquals(new PostSummaryReader.Condition(true, PostCategory.IDOL, Hub.SINDORIM, NOW), reader.receivedCondition);
    }

    @Test
    @DisplayName("카드: 지역은 저장한 원본, 지금 교환은 종료 전일 때만, 시각은 +09:00, 내 글 여부")
    void handle_mapsCard() {
        // given
        Instant endsAt = NOW.plus(Duration.ofMinutes(30));
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of(
                row(2L, VIEWER_ID, PostStatus.AVAILABLE, endsAt),
                row(1L, 99L, PostStatus.AVAILABLE, NOW.minus(Duration.ofMinutes(1)))));

        // when
        List<PostSummaryResult> content = handler(reader)
                .handle(new GetPostsQuery(VIEWER_ID, null, null, null, null, 20)).content();

        // then
        PostSummaryResult mine = content.get(0);
        assertEquals("서울특별시 마포구 동교동", mine.regionName());
        assertEquals("post/2/8f14e45f.jpg", mine.thumbnailKey());
        assertTrue(mine.nowExchange());
        assertEquals(OffsetDateTime.parse("2026-11-02T15:00:00+09:00"), mine.nowExchangeEndsAt());
        assertEquals(OffsetDateTime.parse("2026-11-02T14:30:00+09:00"), mine.createdAt());
        assertTrue(mine.isMine());

        PostSummaryResult ended = content.get(1);
        assertFalse(ended.nowExchange());
        assertFalse(ended.isMine());
    }

    private static GetPostsHandler handler(FakePostSummaryReader reader) {
        return new GetPostsHandler(reader, new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    private static List<PostSummaryReader.Row> rows(long... ids) {
        return LongStream.of(ids).mapToObj(id -> row(id, 99L, PostStatus.AVAILABLE, null)).toList();
    }

    private static PostSummaryReader.Row row(Long id, Long authorId, PostStatus status, Instant nowExchangeEndsAt) {
        return new PostSummaryReader.Row(id, authorId, "post/" + id + "/8f14e45f.jpg", "치이카와 하치와레 가챠 키링",
                "서울특별시 마포구 동교동", status, nowExchangeEndsAt, null, NOW);
    }
}
