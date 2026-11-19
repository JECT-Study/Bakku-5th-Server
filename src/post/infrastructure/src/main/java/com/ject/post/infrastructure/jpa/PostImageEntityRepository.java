package com.ject.post.infrastructure.jpa;

import com.ject.post.infrastructure.entity.PostImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostImageEntityRepository extends JpaRepository<PostImageEntity, Long> {

    List<PostImageEntity> findByPostIdOrderBySortOrder(Long postId);

    List<PostImageEntity> findByPostIdInAndSortOrder(Collection<Long> postIds, int sortOrder);

    @Modifying(flushAutomatically = true)
    @Query("delete from PostImageEntity i where i.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
