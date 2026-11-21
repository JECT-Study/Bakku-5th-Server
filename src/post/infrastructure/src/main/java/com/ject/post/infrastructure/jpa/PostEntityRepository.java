package com.ject.post.infrastructure.jpa;

import com.ject.post.infrastructure.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostEntityRepository extends JpaRepository<PostEntity, Long> {

    @Modifying
    @Query("update PostEntity p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int increaseViewCount(@Param("id") Long id);
}
