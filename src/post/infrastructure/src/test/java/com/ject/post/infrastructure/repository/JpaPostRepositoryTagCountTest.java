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

@Import({JpaPostRepository.class, JpaAuditingConfig.class})
class JpaPostRepositoryTagCountTest extends PostgresTest {

    private static final Long AUTHOR_ID = 3L;
    private static final Instant NOW = Instant.parse("2026-11-25T03:00:00Z");
    private static final Place PLACE = new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null);

    @Autowired
    private JpaPostRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("글을 쓰면 단 태그마다 글 수가 1 늘고, 다른 글과 같이 쓰는 태그는 합쳐 센다")
    void save_new_increasesCounts() {
        repository.save(post(List.of("치이카와", "키링")));
        repository.save(post(List.of("치이카와")));

        assertEquals(2, postCount("치이카와"));
        assertEquals(1, postCount("키링"));
    }

    @Test
    @DisplayName("수정에서 빠진 태그는 1 줄고, 새로 단 태그는 1 늘고, 그대로인 태그는 그대로")
    void save_update_changesOnlyDiff() {
        Post saved = repository.save(post(List.of("치이카와", "키링")));
        repository.save(post(List.of("키링")));

        Post found = repository.findById(saved.getId()).orElseThrow();
        found.update(AUTHOR_ID, List.of("post/tmp/a.jpg"), "치이카와 인형", PostCategory.CHARACTER, false,
                List.of("키링", "인형"), null, null, PLACE);
        repository.save(found);

        assertEquals(0, postCount("치이카와"));
        assertEquals(2, postCount("키링"));
        assertEquals(1, postCount("인형"));
    }

    @Test
    @DisplayName("수정에서 태그를 모두 지우면 그 태그들의 글 수가 1씩 준다")
    void save_update_removeAllTags() {
        Post saved = repository.save(post(List.of("치이카와", "키링")));

        Post found = repository.findById(saved.getId()).orElseThrow();
        found.update(AUTHOR_ID, List.of("post/tmp/a.jpg"), "치이카와 키링", PostCategory.CHARACTER, false,
                List.of(), null, null, PLACE);
        repository.save(found);

        assertEquals(0, postCount("치이카와"));
        assertEquals(0, postCount("키링"));
    }

    @Test
    @DisplayName("글을 지우면 그 글의 태그마다 글 수가 1 준다")
    void delete_decreasesCounts() {
        Post saved = repository.save(post(List.of("치이카와", "키링")));
        repository.save(post(List.of("치이카와")));

        Post found = repository.findById(saved.getId()).orElseThrow();
        found.delete(AUTHOR_ID, NOW);
        repository.delete(found);

        assertEquals(1, postCount("치이카와"));
        assertEquals(0, postCount("키링"));
    }

    private static Post post(List<String> tags) {
        return Post.create(AUTHOR_ID, List.of("post/tmp/a.jpg"), "치이카와 키링", PostCategory.CHARACTER, false,
                tags, null, null, PLACE, null, NOW);
    }

    private int postCount(String tagName) {
        return ((Number) entityManager.createNativeQuery("SELECT post_count FROM post_tag WHERE name = :name")
                .setParameter("name", tagName)
                .getSingleResult()).intValue();
    }
}
