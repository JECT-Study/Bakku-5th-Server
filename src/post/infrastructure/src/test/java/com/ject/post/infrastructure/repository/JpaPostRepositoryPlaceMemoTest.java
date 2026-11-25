package com.ject.post.infrastructure.repository;

import com.ject.common.jpa.JpaAuditingConfig;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.infrastructure.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import({JpaPostRepository.class, JpaAuditingConfig.class})
class JpaPostRepositoryPlaceMemoTest extends PostgresTest {

    @Autowired
    private JpaPostRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상세 주소 50자를 저장한다")
    void save_memo50() {
        String memo = "가".repeat(50);

        Post saved = repository.save(post(memo));

        assertEquals(memo, storedMemo(saved.getId()));
    }

    @Test
    @DisplayName("상세 주소가 비어 있으면 null로 저장한다")
    void save_blankMemo_storesNull() {
        Post saved = repository.save(post("  "));

        assertNull(storedMemo(saved.getId()));
    }

    private static Post post(String memo) {
        return Post.create(3L, List.of("post/tmp/a.jpg"), "치이카와 키링", PostCategory.CHARACTER, false, List.of(),
                null, null, new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", memo),
                null, Instant.parse("2026-11-25T03:00:00Z"));
    }

    private String storedMemo(Long postId) {
        entityManager.flush();
        return (String) entityManager.createNativeQuery("SELECT place_memo FROM post WHERE id = :id")
                .setParameter("id", postId)
                .getSingleResult();
    }
}
