package com.ject.post.infrastructure.repository;

import com.ject.common.jpa.JpaAuditingConfig;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.infrastructure.PostgresTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({JpaPostRepository.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaPostRepositoryTagCountConcurrencyTest extends PostgresTest {

    private static final Long AUTHOR_ID = 77L;
    private static final String TAG = "동시저장태그";
    private static final int WRITERS = 8;
    private static final Place PLACE = new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null);

    @Autowired
    private JpaPostRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM post_tag_link WHERE post_id IN (SELECT id FROM post WHERE author_id = ?)", AUTHOR_ID);
        jdbcTemplate.update("DELETE FROM post_image WHERE post_id IN (SELECT id FROM post WHERE author_id = ?)", AUTHOR_ID);
        jdbcTemplate.update("DELETE FROM post WHERE author_id = ?", AUTHOR_ID);
        jdbcTemplate.update("DELETE FROM post_tag WHERE name = ?", TAG);
    }

    @Test
    @DisplayName("같은 태그로 글 여러 개를 동시에 저장해도 모두 성공하고 글 수가 정확하다")
    void save_concurrentSameTag_allSucceedAndCountIsExact() throws Exception {
        repository.save(post());
        jdbcTemplate.update("DELETE FROM post_tag_link WHERE post_id IN (SELECT id FROM post WHERE author_id = ?)", AUTHOR_ID);
        jdbcTemplate.update("UPDATE post_tag SET post_count = 0 WHERE name = ?", TAG);

        ExecutorService executor = Executors.newFixedThreadPool(WRITERS);
        CountDownLatch start = new CountDownLatch(1);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < WRITERS; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    repository.save(post());
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
        }
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(List.of(), failures.stream().map(Throwable::toString).toList());
        assertEquals(WRITERS, jdbcTemplate.queryForObject(
                "SELECT post_count FROM post_tag WHERE name = ?", Integer.class, TAG));
    }

    private static Post post() {
        return Post.create(AUTHOR_ID, List.of("post/tmp/a.jpg"), "동시 저장 글", PostCategory.CHARACTER, false,
                List.of(TAG), null, null, PLACE, null, Instant.parse("2026-11-25T03:00:00Z"));
    }
}
