package com.ject.post.infrastructure.jpa;

import com.ject.post.infrastructure.entity.PostTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostTagEntityRepository extends JpaRepository<PostTagEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO post_tag (name, created_at, updated_at)
            VALUES (:name, now(), now())
            ON CONFLICT (name) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("name") String name);

    List<PostTagEntity> findByNameIn(Collection<String> names);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE post_tag
            SET post_count = post_count + :delta
            WHERE id IN (SELECT id FROM post_tag WHERE id IN (:ids) ORDER BY id FOR NO KEY UPDATE)
            """, nativeQuery = true)
    void addPostCount(@Param("ids") Collection<Long> ids, @Param("delta") int delta);
}
