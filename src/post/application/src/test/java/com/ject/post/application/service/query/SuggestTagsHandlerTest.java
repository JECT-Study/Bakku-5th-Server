package com.ject.post.application.service.query;

import com.ject.post.application.dto.TagSuggestionResult;
import com.ject.post.application.dto.query.SuggestTagsQuery;
import com.ject.post.application.port.TagSuggestionReader;
import com.ject.post.domain.exception.InvalidTagKeywordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuggestTagsHandlerTest {

    @Test
    @DisplayName("정규화한 검색어와 size로 조회하고 받은 순서 그대로 돌려준다")
    void handle_passesNormalizedKeywordAndSize() {
        FakeTagSuggestionReader reader = new FakeTagSuggestionReader(List.of("치이카와", "치이카와가챠"));

        TagSuggestionResult result = new SuggestTagsHandler(reader).handle(new SuggestTagsQuery("# 치이 ", 10));

        assertEquals("치이", reader.receivedKeyword);
        assertEquals(10, reader.receivedLimit);
        assertEquals(List.of("치이카와", "치이카와가챠"), result.tags());
    }

    @Test
    @DisplayName("맞는 태그가 없으면 빈 목록")
    void handle_noMatch_empty() {
        FakeTagSuggestionReader reader = new FakeTagSuggestionReader(List.of());

        TagSuggestionResult result = new SuggestTagsHandler(reader).handle(new SuggestTagsQuery("모몽가", 10));

        assertEquals(List.of(), result.tags());
    }

    @Test
    @DisplayName("정규화한 뒤 1~20자가 아니면 POST_010, 조회하지 않는다")
    void handle_invalidKeyword_throws() {
        FakeTagSuggestionReader reader = new FakeTagSuggestionReader(List.of());

        assertThrows(InvalidTagKeywordException.class,
                () -> new SuggestTagsHandler(reader).handle(new SuggestTagsQuery("# ", 10)));
        assertNull(reader.receivedKeyword);
    }

    private static class FakeTagSuggestionReader implements TagSuggestionReader {

        private final List<String> tags;
        private String receivedKeyword;
        private int receivedLimit;

        FakeTagSuggestionReader(List<String> tags) {
            this.tags = tags;
        }

        @Override
        public List<String> suggest(String keyword, int limit) {
            this.receivedKeyword = keyword;
            this.receivedLimit = limit;
            return tags;
        }
    }
}
