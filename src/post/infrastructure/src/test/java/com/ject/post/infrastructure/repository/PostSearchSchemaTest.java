package com.ject.post.infrastructure.repository;

import com.ject.common.jpa.JpaAuditingConfig;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.infrastructure.PostgresTest;
import com.ject.post.infrastructure.entity.PostEntity;
import com.ject.post.infrastructure.jpa.PostEntityRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(JpaAuditingConfig.class)
class PostSearchSchemaTest extends PostgresTest {

    @Autowired
    private PostEntityRepository postEntityRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @DisplayName("제품명을 저장하면 DB가 띄어쓰기를 지우고 소문자로 바꾼 값을 자동으로 채운다")
    @CsvSource(delimiter = '|', value = {
            "치이카와 하치와레 키링|치이카와하치와레키링",
            "'  Chiikawa   KEYRING  '|chiikawakeyring",
            "'치이카와\t키링'|치이카와키링"
    })
    void titleNormalized_onInsert(String title, String expected) {
        Long id = save(title);

        assertEquals(expected, titleNormalized(id));
    }

    @ParameterizedTest(name = "수정 후 \"{0}\"")
    @DisplayName("제품명을 수정하면 정규화 값도 다시 계산된다")
    @CsvSource({"하치 와레 인형, 하치와레인형"})
    void titleNormalized_onUpdate(String newTitle, String expected) {
        Long id = save("치이카와 키링");

        entityManager.createNativeQuery("UPDATE post SET title = :title WHERE id = :id")
                .setParameter("title", newTitle)
                .setParameter("id", id)
                .executeUpdate();

        assertEquals(expected, titleNormalized(id));
    }

    private Long save(String title) {
        Post post = Post.create(3L, List.of("post/tmp/3/a.jpg"), title, PostCategory.CHARACTER, false, List.of(),
                null, null, new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null),
                null, Instant.parse("2026-11-24T05:00:00Z"));
        PostEntity saved = postEntityRepository.saveAndFlush(PostEntity.from(post));
        return saved.getId();
    }

    private String titleNormalized(Long id) {
        return (String) entityManager.createNativeQuery("SELECT title_normalized FROM post WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();
    }
}
