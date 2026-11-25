package com.ject.post.infrastructure.repository;

import com.ject.post.domain.domain.PostStatus;
import com.ject.post.infrastructure.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(JpaTagSuggestionReader.class)
class JpaTagSuggestionReaderTest extends PostgresTest {

    @Autowired
    private JpaTagSuggestionReader reader;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("입력 글자가 들어간 태그를 글 수 많은 순, 같으면 이름순으로")
    void suggest_orderByPostCountThenName() {
        insert(PostStatus.AVAILABLE, "치이카와", "치이카와굿즈");
        insert(PostStatus.AVAILABLE, "치이카와");
        insert(PostStatus.AVAILABLE, "치이카와");
        insert(PostStatus.AVAILABLE, "먼작귀치이카와");
        insert(PostStatus.AVAILABLE, "하치와레");

        assertEquals(List.of("치이카와", "먼작귀치이카와", "치이카와굿즈"), reader.suggest("치이", 10));
    }

    @Test
    @DisplayName("삭제 글은 세지 않고 예약중 · 교환 완료 글은 센다")
    void suggest_countsAllButDeleted() {
        insert(PostStatus.DELETED, "가챠");
        insert(PostStatus.DELETED, "가챠");
        insert(PostStatus.DELETED, "가챠");
        insert(PostStatus.RESERVED, "가챠키링");
        insert(PostStatus.COMPLETED, "가챠키링");
        insert(PostStatus.AVAILABLE, "가챠");

        assertEquals(List.of("가챠키링", "가챠"), reader.suggest("가챠", 10));
    }

    @Test
    @DisplayName("글이 0개인 태그는 추천하지 않는다 (삭제 글에만 달린 태그 포함)")
    void suggest_excludesTagsWithoutPosts() {
        insertTag("모몽가인형");
        insert(PostStatus.DELETED, "모몽가키링");
        insert(PostStatus.AVAILABLE, "모몽가");

        assertEquals(List.of("모몽가"), reader.suggest("모몽", 10));
    }

    @Test
    @DisplayName("limit개까지만")
    void suggest_limit() {
        insert(PostStatus.AVAILABLE, "포카1", "포카2", "포카3");

        assertEquals(List.of("포카1", "포카2"), reader.suggest("포카", 2));
    }

    @Test
    @DisplayName("검색어의 % · _는 패턴 문자가 아니라 글자 그대로 찾는다")
    void suggest_escapesLikeWildcards() {
        insert(PostStatus.AVAILABLE, "100%정품", "a_b", "axb");

        assertEquals(List.of("100%정품"), reader.suggest("0%", 10));
        assertEquals(List.of("a_b"), reader.suggest("a_b", 10));
    }

    private void insert(PostStatus status, String... tags) {
        Long id = ((Number) entityManager.createNativeQuery("""
                        INSERT INTO post (author_id, status, title, category, is_opened, place_lat, place_lng,
                                          place_name, place_region_name, view_count, created_at, updated_at, hub, version)
                        VALUES (3, :status, '태그 테스트 글', 'CHARACTER', false, 37.5563, 126.9236,
                                '스타벅스 홍대동교점', '서울특별시 마포구 동교동', 0, now(), now(), 'HONGDAE', 0)
                        RETURNING id
                        """)
                .setParameter("status", status.name())
                .getSingleResult()).longValue();
        for (int i = 0; i < tags.length; i++) {
            insertTag(tags[i]);
            entityManager.createNativeQuery("""
                            INSERT INTO post_tag_link (post_id, tag_id, sort_order, created_at, updated_at)
                            SELECT :postId, id, :sortOrder, now(), now() FROM post_tag WHERE name = :name
                            """)
                    .setParameter("postId", id)
                    .setParameter("sortOrder", i + 1)
                    .setParameter("name", tags[i])
                    .executeUpdate();
        }
    }

    private void insertTag(String name) {
        entityManager.createNativeQuery("""
                        INSERT INTO post_tag (name, created_at, updated_at) VALUES (:name, now(), now())
                        ON CONFLICT (name) DO NOTHING
                        """)
                .setParameter("name", name)
                .executeUpdate();
    }
}
