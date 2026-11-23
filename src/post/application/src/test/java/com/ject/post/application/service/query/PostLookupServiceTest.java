package com.ject.post.application.service.query;

import com.ject.post.application.FakePostSummaryReader;
import com.ject.post.application.contract.PostBrief;
import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostLookupServiceTest {

    @Test
    @DisplayName("요청한 id 순서대로 작성자 · 상태 · 제목 · 대표 사진 key를 돌려준다")
    void findAllByIds_keepsRequestOrder() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of(), List.of(
                brief(1L, 100L, PostStatus.AVAILABLE),
                brief(2L, 200L, PostStatus.RESERVED),
                brief(3L, 200L, PostStatus.COMPLETED)));

        // when
        List<PostBrief> result = new PostLookupService(reader).findAllByIds(List.of(3L, 1L, 2L));

        // then
        assertEquals(List.of(3L, 1L, 2L), result.stream().map(PostBrief::id).toList());
        assertEquals(new PostBrief(3L, 200L, "COMPLETED", "제목 3", "post/3/a.jpg"), result.get(0));
    }

    @Test
    @DisplayName("삭제된 글도 DELETED로 돌려주고, 없는 id는 결과에서 뺀다")
    void findAllByIds_includesDeleted_skipsMissing() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of(), List.of(
                brief(1L, 100L, PostStatus.DELETED)));

        // when
        List<PostBrief> result = new PostLookupService(reader).findAllByIds(List.of(1L, 99L));

        // then
        assertEquals(1, result.size());
        assertEquals("DELETED", result.get(0).status());
    }

    @Test
    @DisplayName("중복 id는 한 번만 조회하고 한 번만 돌려준다")
    void findAllByIds_duplicateIds_once() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of(), List.of(
                brief(1L, 100L, PostStatus.AVAILABLE),
                brief(2L, 100L, PostStatus.AVAILABLE)));

        // when
        List<PostBrief> result = new PostLookupService(reader).findAllByIds(List.of(2L, 1L, 2L));

        // then
        assertEquals(List.of(2L, 1L), reader.receivedIds);
        assertEquals(List.of(2L, 1L), result.stream().map(PostBrief::id).toList());
    }

    @Test
    @DisplayName("대표 사진이 없으면 thumbnailKey는 null이다")
    void findAllByIds_noThumbnail_null() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of(), List.of(
                new PostSummaryReader.Brief(1L, 100L, PostStatus.AVAILABLE, "제목 1", null)));

        // when
        List<PostBrief> result = new PostLookupService(reader).findAllByIds(List.of(1L));

        // then
        assertNull(result.get(0).thumbnailKey());
    }

    @Test
    @DisplayName("빈 목록이면 조회하지 않고 빈 결과를 돌려준다")
    void findAllByIds_empty_noQuery() {
        // given
        FakePostSummaryReader reader = new FakePostSummaryReader(List.of());

        // when
        List<PostBrief> result = new PostLookupService(reader).findAllByIds(List.of());

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, reader.findBriefsCalls);
    }

    private static PostSummaryReader.Brief brief(Long id, Long authorId, PostStatus status) {
        return new PostSummaryReader.Brief(id, authorId, status, "제목 " + id, "post/" + id + "/a.jpg");
    }
}
