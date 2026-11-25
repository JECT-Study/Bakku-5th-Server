package com.ject.post.infrastructure.repository;

import com.ject.common.jpa.JpaAuditingConfig;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
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
class JpaPostRepositoryWithdrawTest extends PostgresTest {

    private static final Long WITHDRAWN_ID = 3L;
    private static final Long OTHER_ID = 4L;
    private static final Instant NOW = Instant.parse("2026-11-25T03:00:00Z");
    private static final Place PLACE = new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null);

    @Autowired
    private JpaPostRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("탈퇴 회원의 교환 가능 글만 삭제하고, 예약중 · 교환 완료 · 다른 회원 글은 그대로 둔다")
    void deleteAvailableByAuthor_onlyAvailable() {
        Long available1 = save(WITHDRAWN_ID, List.of());
        Long available2 = save(WITHDRAWN_ID, List.of());
        Long reserved = save(WITHDRAWN_ID, List.of());
        Long completed = save(WITHDRAWN_ID, List.of());
        Long other = save(OTHER_ID, List.of());
        setStatus(reserved, PostStatus.RESERVED);
        setStatus(completed, PostStatus.COMPLETED);

        int deleted = repository.deleteAvailableByAuthor(WITHDRAWN_ID, NOW);

        assertEquals(2, deleted);
        assertEquals("DELETED", status(available1));
        assertEquals("DELETED", status(available2));
        assertEquals(NOW, deletedAt(available1));
        assertEquals("RESERVED", status(reserved));
        assertEquals("COMPLETED", status(completed));
        assertEquals("AVAILABLE", status(other));
        assertNull(deletedAt(other));
    }

    @Test
    @DisplayName("삭제한 글 수만큼 태그 글 수가 준다")
    void deleteAvailableByAuthor_decreasesTagCounts() {
        save(WITHDRAWN_ID, List.of("치이카와", "키링"));
        save(WITHDRAWN_ID, List.of("치이카와"));
        save(OTHER_ID, List.of("치이카와"));
        Long reserved = save(WITHDRAWN_ID, List.of("키링"));
        setStatus(reserved, PostStatus.RESERVED);

        repository.deleteAvailableByAuthor(WITHDRAWN_ID, NOW);

        assertEquals(1, postCount("치이카와"));
        assertEquals(1, postCount("키링"));
    }

    @Test
    @DisplayName("지울 글이 없으면 0")
    void deleteAvailableByAuthor_none() {
        save(OTHER_ID, List.of("치이카와"));

        assertEquals(0, repository.deleteAvailableByAuthor(WITHDRAWN_ID, NOW));
        assertEquals(1, postCount("치이카와"));
    }

    private Long save(Long authorId, List<String> tags) {
        return repository.save(Post.create(authorId, List.of("post/tmp/a.jpg"), "치이카와 키링",
                PostCategory.CHARACTER, false, tags, null, null, PLACE, null, NOW)).getId();
    }

    private void setStatus(Long postId, PostStatus status) {
        entityManager.createNativeQuery("UPDATE post SET status = :status WHERE id = :id")
                .setParameter("status", status.name())
                .setParameter("id", postId)
                .executeUpdate();
    }

    private String status(Long postId) {
        return (String) entityManager.createNativeQuery("SELECT status FROM post WHERE id = :id")
                .setParameter("id", postId)
                .getSingleResult();
    }

    private Instant deletedAt(Long postId) {
        return (Instant) entityManager.createNativeQuery("SELECT deleted_at FROM post WHERE id = :id", Instant.class)
                .setParameter("id", postId)
                .getSingleResult();
    }

    private int postCount(String tagName) {
        return ((Number) entityManager.createNativeQuery("SELECT post_count FROM post_tag WHERE name = :name")
                .setParameter("name", tagName)
                .getSingleResult()).intValue();
    }
}
