package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.infrastructure.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(JpaPostSummaryReader.class)
class JpaPostSummaryReaderTest extends PostgresTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private JpaPostSummaryReader reader;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("홈 목록: 교환 가능 글만 최신순, 거점을 주면 그 거점 글만")
    void findHomePage_hubFilter() {
        Long hongdae1 = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.CHARACTER, null);
        Long sindorim = insert(PostStatus.AVAILABLE, Hub.SINDORIM, PostCategory.CHARACTER, null);
        insert(PostStatus.RESERVED, Hub.HONGDAE, PostCategory.CHARACTER, null);
        insert(PostStatus.DELETED, Hub.HONGDAE, PostCategory.CHARACTER, null);
        Long hongdae2 = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.CHARACTER, null);

        assertEquals(List.of(hongdae2, sindorim, hongdae1), ids(reader.findHomePage(condition(null, null, null), null, 10)));
        assertEquals(List.of(hongdae2, hongdae1), ids(reader.findHomePage(condition(null, null, Hub.HONGDAE), null, 10)));
    }

    @Test
    @DisplayName("홈 목록: 카테고리 · 지금 교환(종료 전만) 조건")
    void findHomePage_chips() {
        Long nowIdol = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.IDOL, NOW.plusSeconds(600));
        Long endedIdol = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.IDOL, NOW.minusSeconds(600));
        Long normalEtc = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.ETC, null);

        assertEquals(List.of(endedIdol, nowIdol), ids(reader.findHomePage(condition(null, PostCategory.IDOL, null), null, 10)));
        assertEquals(List.of(nowIdol), ids(reader.findHomePage(condition(true, null, null), null, 10)));
        assertEquals(List.of(normalEtc, endedIdol), ids(reader.findHomePage(condition(false, null, null), null, 10)));
    }

    @Test
    @DisplayName("홈 목록: 커서보다 작은 id부터 limit개, 대표 사진은 1번 사진")
    void findHomePage_cursorAndThumbnail() {
        Long first = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.ETC, null);
        Long second = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.ETC, null);
        Long third = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.ETC, null);

        List<PostSummaryReader.Row> page = reader.findHomePage(condition(null, null, null), third, 1);

        assertEquals(List.of(second), ids(page));
        assertEquals("post/" + second + "/1.jpg", page.get(0).thumbnailKey());
        assertEquals(List.of(first), ids(reader.findHomePage(condition(null, null, null), second, 10)));
    }

    @Test
    @DisplayName("요약 일괄 조회: 삭제 글도 포함하고 대표 사진 key를 붙인다")
    void findBriefs_includesDeleted() {
        Long available = insert(PostStatus.AVAILABLE, Hub.HONGDAE, PostCategory.ETC, null);
        Long deleted = insert(PostStatus.DELETED, Hub.HONGDAE, PostCategory.ETC, null);

        List<PostSummaryReader.Brief> briefs = reader.findBriefs(List.of(available, deleted, 999_999L));

        assertEquals(2, briefs.size());
        PostSummaryReader.Brief deletedBrief = briefs.stream().filter(b -> b.id().equals(deleted)).findFirst().orElseThrow();
        assertEquals(PostStatus.DELETED, deletedBrief.status());
        assertEquals("post/" + deleted + "/1.jpg", deletedBrief.thumbnailKey());
    }

    private static PostSummaryReader.Condition condition(Boolean nowExchange, PostCategory category, Hub hub) {
        return new PostSummaryReader.Condition(nowExchange, category, hub, NOW);
    }

    private static List<Long> ids(List<?> rows) {
        return rows.stream()
                .map(row -> row instanceof PostSummaryReader.Row r ? r.id() : ((PostSummaryReader.Brief) row).id())
                .toList();
    }

    private Long insert(PostStatus status, Hub hub, PostCategory category, Instant nowExchangeEndsAt) {
        Long id = ((Number) entityManager.createNativeQuery("""
                        INSERT INTO post (author_id, status, title, category, is_opened, place_lat, place_lng,
                                          place_name, place_region_name, now_exchange_ends_at, view_count,
                                          created_at, updated_at, hub, version)
                        VALUES (3, :status, '치이카와 키링', :category, false, 37.5563, 126.9236,
                                '스타벅스 홍대동교점', '서울특별시 마포구 동교동', :endsAt, 0, now(), now(), :hub, 0)
                        RETURNING id
                        """)
                .setParameter("status", status.name())
                .setParameter("category", category.name())
                .setParameter("endsAt", nowExchangeEndsAt)
                .setParameter("hub", hub.name())
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO post_image (post_id, image_key, sort_order, created_at, updated_at)
                        VALUES (:id, 'post/' || :id || '/1.jpg', 1, now(), now()),
                               (:id, 'post/' || :id || '/2.jpg', 2, now(), now())
                        """)
                .setParameter("id", id)
                .executeUpdate();
        return id;
    }
}
