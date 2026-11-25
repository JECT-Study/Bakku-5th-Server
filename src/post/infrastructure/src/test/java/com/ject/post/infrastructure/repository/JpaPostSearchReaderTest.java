package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.PostSearchReader;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(JpaPostSearchReader.class)
class JpaPostSearchReaderTest extends PostgresTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private JpaPostSearchReader reader;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("정확도순: 태그 정확 일치(1) → 제품명 포함(2) → 태그 일부 포함(3), 같은 순위는 최신순")
    void search_ranking() {
        Long tagExactOld = insert("가챠 피규어", PostStatus.AVAILABLE, PostCategory.CHARACTER, "치이카와");
        Long titleContains = insert("치이카와 키링", PostStatus.AVAILABLE, PostCategory.CHARACTER);
        Long tagContains = insert("인형", PostStatus.AVAILABLE, PostCategory.CHARACTER, "치이카와굿즈");
        insert("하치와레 키링", PostStatus.AVAILABLE, PostCategory.CHARACTER, "하치와레");
        Long tagExactNew = insert("치이카와 인형", PostStatus.AVAILABLE, PostCategory.CHARACTER, "치이카와");

        List<PostSearchReader.Hit> hits = reader.search(condition("치이카와", null), null, 10);

        assertEquals(List.of(tagExactNew, tagExactOld, titleContains, tagContains), ids(hits));
        assertEquals(List.of(1, 1, 2, 3), hits.stream().map(PostSearchReader.Hit::rank).toList());
    }

    @Test
    @DisplayName("제품명은 띄어쓰기 · 대소문자를 무시하고 찾는다 (검색어는 정규화해서 들어옴)")
    void search_titleIgnoresSpacesAndCase() {
        Long spaced = insert("치이 카와 KEY ring", PostStatus.AVAILABLE, PostCategory.CHARACTER);

        assertEquals(List.of(spaced), ids(reader.search(condition("치이카와keyring", null), null, 10)));
    }

    @Test
    @DisplayName("교환 가능 글만, 카테고리를 주면 그 카테고리만")
    void search_statusAndCategory() {
        Long idol = insert("치이카와 포토카드", PostStatus.AVAILABLE, PostCategory.IDOL);
        Long character = insert("치이카와 키링", PostStatus.AVAILABLE, PostCategory.CHARACTER);
        insert("치이카와 인형", PostStatus.RESERVED, PostCategory.CHARACTER);
        insert("치이카와 쿠션", PostStatus.DELETED, PostCategory.CHARACTER);

        assertEquals(List.of(character, idol), ids(reader.search(condition("치이카와", null), null, 10)));
        assertEquals(List.of(idol), ids(reader.search(condition("치이카와", PostCategory.IDOL), null, 10)));
    }

    @Test
    @DisplayName("태그로만 걸린 글도 교환 가능 · 카테고리 조건을 따른다")
    void search_tagOnlyFollowsStatusAndCategory() {
        Long character = insert("인형", PostStatus.AVAILABLE, PostCategory.CHARACTER, "치이카와");
        Long idol = insert("포토카드", PostStatus.AVAILABLE, PostCategory.IDOL, "치이카와굿즈");
        insert("쿠션", PostStatus.RESERVED, PostCategory.CHARACTER, "치이카와");
        insert("키링", PostStatus.DELETED, PostCategory.CHARACTER, "치이카와굿즈");

        assertEquals(List.of(character, idol), ids(reader.search(condition("치이카와", null), null, 10)));
        assertEquals(List.of(character), ids(reader.search(condition("치이카와", PostCategory.CHARACTER), null, 10)));
    }

    @Test
    @DisplayName("커서로 10개씩 이어 받으면 30개가 빠짐 · 중복 없이 정확도순으로 나온다")
    void search_cursorPagination() {
        List<Long> expected = new ArrayList<>();
        List<Long> rank1 = new ArrayList<>();
        List<Long> rank2 = new ArrayList<>();
        List<Long> rank3 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            rank2.add(insert("치이카와 키링 " + i, PostStatus.AVAILABLE, PostCategory.CHARACTER));
            rank1.add(insert("가챠 " + i, PostStatus.AVAILABLE, PostCategory.CHARACTER, "치이카와"));
            rank3.add(insert("인형 " + i, PostStatus.AVAILABLE, PostCategory.CHARACTER, "치이카와굿즈"));
        }
        expected.addAll(rank1.reversed());
        expected.addAll(rank2.reversed());
        expected.addAll(rank3.reversed());

        List<Long> collected = new ArrayList<>();
        PostSearchReader.Cursor cursor = null;
        for (int page = 0; page < 4; page++) {
            List<PostSearchReader.Hit> hits = reader.search(condition("치이카와", null), cursor, 10);
            if (hits.isEmpty()) {
                break;
            }
            collected.addAll(ids(hits));
            PostSearchReader.Hit last = hits.get(hits.size() - 1);
            cursor = new PostSearchReader.Cursor(last.rank(), last.row().id());
        }

        assertEquals(expected, collected);
        assertEquals(30, new HashSet<>(collected).size());
    }

    @Test
    @DisplayName("검색어의 % · _는 패턴 문자가 아니라 글자 그대로 찾는다")
    void search_escapesLikeWildcards() {
        Long percent = insert("할인100%키링", PostStatus.AVAILABLE, PostCategory.ETC);
        Long underscore = insert("a_b키링", PostStatus.AVAILABLE, PostCategory.ETC);
        insert("axb키링", PostStatus.AVAILABLE, PostCategory.ETC);

        assertEquals(List.of(percent), ids(reader.search(condition("100%키", null), null, 10)));
        assertEquals(List.of(underscore), ids(reader.search(condition("a_b", null), null, 10)));
    }

    @Test
    @DisplayName("대표 사진은 1번 사진 key")
    void search_thumbnail() {
        Long id = insert("치이카와 키링", PostStatus.AVAILABLE, PostCategory.CHARACTER);

        PostSearchReader.Hit hit = reader.search(condition("치이카와", null), null, 10).get(0);

        assertEquals("post/" + id + "/1.jpg", hit.row().thumbnailKey());
        assertTrue(hit.row().title().startsWith("치이카와"));
    }

    @Test
    @DisplayName("1글자 검색어도 제품명 · 태그에서 찾는다")
    void search_oneCharacter() {
        Long title = insert("치이카와 키링", PostStatus.AVAILABLE, PostCategory.CHARACTER);
        Long tag = insert("인형", PostStatus.AVAILABLE, PostCategory.CHARACTER, "치즈");
        insert("모몽가 키링", PostStatus.AVAILABLE, PostCategory.CHARACTER, "모몽가");

        assertEquals(List.of(title, tag), ids(reader.search(condition("치", null), null, 10)));
    }

    @Test
    @DisplayName("거점을 주면 그 거점 글만 (태그로만 걸린 글 포함), 생략하면 모든 지역")
    void search_hubFilter() {
        Long hongdae = insertAt("치이카와 키링", Hub.HONGDAE, null);
        Long sindorimTitle = insertAt("치이카와 인형", Hub.SINDORIM, null);
        Long sindorimTag = insertAt("쿠션", Hub.SINDORIM, null, "치이카와");

        assertEquals(List.of(sindorimTag, sindorimTitle),
                ids(reader.search(condition("치이카와", null, Hub.SINDORIM, null), null, 10)));
        assertEquals(List.of(sindorimTag, sindorimTitle, hongdae),
                ids(reader.search(condition("치이카와", null, null, null), null, 10)));
    }

    @Test
    @DisplayName("지금 교환은 남은 시간이 있는 글만, 일반 교환은 그 외 (시간이 끝난 지금 교환 글 포함)")
    void search_nowExchangeFilter() {
        Long now = insertAt("치이카와 키링", Hub.HONGDAE, NOW.plusSeconds(3600));
        Long ended = insertAt("치이카와 인형", Hub.HONGDAE, NOW.minusSeconds(3600));
        Long normal = insertAt("치이카와 쿠션", Hub.HONGDAE, null);

        assertEquals(List.of(now), ids(reader.search(condition("치이카와", null, null, true), null, 10)));
        assertEquals(List.of(normal, ended), ids(reader.search(condition("치이카와", null, null, false), null, 10)));
    }

    private static PostSearchReader.Condition condition(String keyword, PostCategory category) {
        return condition(keyword, category, null, null);
    }

    private static PostSearchReader.Condition condition(String keyword, PostCategory category, Hub hub,
                                                        Boolean nowExchange) {
        return new PostSearchReader.Condition(keyword, category, hub, nowExchange, NOW);
    }

    private Long insertAt(String title, Hub hub, Instant nowExchangeEndsAt, String... tags) {
        Long id = insert(title, PostStatus.AVAILABLE, PostCategory.CHARACTER, tags);
        entityManager.createNativeQuery("UPDATE post SET hub = :hub WHERE id = :id")
                .setParameter("hub", hub.name())
                .setParameter("id", id)
                .executeUpdate();
        if (nowExchangeEndsAt != null) {
            entityManager.createNativeQuery("UPDATE post SET now_exchange_ends_at = :endsAt WHERE id = :id")
                    .setParameter("endsAt", nowExchangeEndsAt)
                    .setParameter("id", id)
                    .executeUpdate();
        }
        return id;
    }

    private static List<Long> ids(List<PostSearchReader.Hit> hits) {
        return hits.stream().map(hit -> hit.row().id()).toList();
    }

    private Long insert(String title, PostStatus status, PostCategory category, String... tags) {
        Long id = ((Number) entityManager.createNativeQuery("""
                        INSERT INTO post (author_id, status, title, category, is_opened, place_lat, place_lng,
                                          place_name, place_region_name, view_count, created_at, updated_at, hub, version)
                        VALUES (3, :status, :title, :category, false, 37.5563, 126.9236,
                                '스타벅스 홍대동교점', '서울특별시 마포구 동교동', 0, now(), now(), 'HONGDAE', 0)
                        RETURNING id
                        """)
                .setParameter("status", status.name())
                .setParameter("title", title)
                .setParameter("category", category.name())
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        INSERT INTO post_image (post_id, image_key, sort_order, created_at, updated_at)
                        VALUES (:id, 'post/' || :id || '/1.jpg', 1, now(), now())
                        """)
                .setParameter("id", id)
                .executeUpdate();
        for (int i = 0; i < tags.length; i++) {
            entityManager.createNativeQuery("""
                            INSERT INTO post_tag (name, created_at, updated_at) VALUES (:name, now(), now())
                            ON CONFLICT (name) DO NOTHING
                            """)
                    .setParameter("name", tags[i])
                    .executeUpdate();
            entityManager.createNativeQuery("""
                            INSERT INTO post_tag_link (post_id, tag_id, sort_order, created_at, updated_at)
                            SELECT :postId, id, :sortOrder, now(), now() FROM post_tag WHERE name = :name
                            """)
                    .setParameter("postId", id)
                    .setParameter("sortOrder", i + 1)
                    .setParameter("name", tags[i])
                    .executeUpdate();
        }
        return id;
    }
}
