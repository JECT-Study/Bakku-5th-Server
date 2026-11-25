package com.ject.post.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.FakeBlockRelationPort;
import com.ject.post.application.dto.PostSearchPageResult;
import com.ject.post.application.dto.PostSummaryResult;
import com.ject.post.application.dto.query.SearchPostsQuery;
import com.ject.post.application.port.PostSearchReader;
import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.InvalidSearchKeywordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchPostsHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-24T05:00:00Z");
    private static final Long VIEWER_ID = 3L;

    @Test
    @DisplayName("정규화한 검색어 · 카테고리 · 거점 · 교환 방식 · 지금 시각 · 커서로 size + 1개를 요청한다")
    void handle_passesConditionAndCursor() {
        FakePostSearchReader reader = new FakePostSearchReader(List.of());

        handler(reader).handle(new SearchPostsQuery(VIEWER_ID, "# 치이 카와", "IDOL", "SINDORIM", true, 2, 57L, 20));

        assertEquals(new PostSearchReader.Condition("치이카와", PostCategory.IDOL, Hub.SINDORIM, true, NOW, List.of()),
                reader.receivedCondition);
        assertEquals(new PostSearchReader.Cursor(2, 57L), reader.receivedCursor);
        assertEquals(21, reader.receivedLimit);
    }

    @Test
    @DisplayName("내가 차단했거나 나를 차단한 회원을 제외할 작성자로 넘긴다")
    void handle_passesBlockRelatedAuthors() {
        FakePostSearchReader reader = new FakePostSearchReader(List.of());
        FakeBlockRelationPort blockRelationPort = new FakeBlockRelationPort().block(VIEWER_ID, 7L).block(9L, VIEWER_ID);

        handler(reader, blockRelationPort).handle(
                new SearchPostsQuery(VIEWER_ID, "치이카와", null, null, null, null, null, 20));

        assertEquals(List.of(7L, 9L), reader.receivedCondition.excludedAuthorIds());
    }

    @Test
    @DisplayName("첫 페이지는 커서 없이 요청한다")
    void handle_firstPage_noCursor() {
        FakePostSearchReader reader = new FakePostSearchReader(List.of());

        handler(reader).handle(new SearchPostsQuery(VIEWER_ID, "치이카와", null, null, null, null, null, 20));

        assertNull(reader.receivedCursor);
        assertNull(reader.receivedCondition.category());
        assertNull(reader.receivedCondition.hub());
        assertNull(reader.receivedCondition.nowExchange());
    }

    @Test
    @DisplayName("더 있으면 size개만 주고 마지막 글의 순위 · id를 다음 커서로 준다")
    void handle_hasNext() {
        FakePostSearchReader reader = new FakePostSearchReader(List.of(hit(1, 30L), hit(2, 29L), hit(3, 28L)));

        PostSearchPageResult result = handler(reader).handle(
                new SearchPostsQuery(VIEWER_ID, "치이카와", null, null, null, null, null, 2));

        assertEquals(List.of(30L, 29L), result.content().stream().map(PostSummaryResult::id).toList());
        assertTrue(result.hasNext());
        assertEquals(2, result.nextCursorRank());
        assertEquals(29L, result.nextCursorId());
    }

    @Test
    @DisplayName("마지막 페이지면 다음 커서가 없다")
    void handle_lastPage() {
        FakePostSearchReader reader = new FakePostSearchReader(List.of(hit(1, 30L)));

        PostSearchPageResult result = handler(reader).handle(
                new SearchPostsQuery(VIEWER_ID, "치이카와", null, null, null, null, null, 2));

        assertFalse(result.hasNext());
        assertNull(result.nextCursorRank());
        assertNull(result.nextCursorId());
    }

    @Test
    @DisplayName("정규화한 뒤 비어 있으면 POST_009, 조회하지 않는다")
    void handle_invalidKeyword_throws() {
        FakePostSearchReader reader = new FakePostSearchReader(List.of());

        assertThrows(InvalidSearchKeywordException.class,
                () -> handler(reader).handle(new SearchPostsQuery(VIEWER_ID, "# ", null, null, null, null, null, 20)));
        assertNull(reader.receivedCondition);
    }

    private static SearchPostsHandler handler(FakePostSearchReader reader) {
        return handler(reader, new FakeBlockRelationPort());
    }

    private static SearchPostsHandler handler(FakePostSearchReader reader, FakeBlockRelationPort blockRelationPort) {
        return new SearchPostsHandler(reader, blockRelationPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    private static PostSearchReader.Hit hit(int rank, Long id) {
        return new PostSearchReader.Hit(rank, new PostSummaryReader.Row(id, 99L, "post/" + id + "/a.jpg", "치이카와 키링",
                "서울특별시 마포구 동교동", PostStatus.AVAILABLE, null, null, NOW));
    }

    private static class FakePostSearchReader implements PostSearchReader {

        private final List<Hit> hits;
        private Condition receivedCondition;
        private Cursor receivedCursor;
        private int receivedLimit;

        FakePostSearchReader(List<Hit> hits) {
            this.hits = hits;
        }

        @Override
        public List<Hit> search(Condition condition, Cursor cursor, int limit) {
            this.receivedCondition = condition;
            this.receivedCursor = cursor;
            this.receivedLimit = limit;
            return hits.stream().limit(limit).toList();
        }
    }
}
