package com.ject.post.infrastructure.jpa;

import com.ject.post.infrastructure.entity.PostTagLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostTagLinkEntityRepository extends JpaRepository<PostTagLinkEntity, Long> {

    @Query("""
            select t.name
            from PostTagLinkEntity l
            join PostTagEntity t on t.id = l.tagId
            where l.postId = :postId
            order by l.sortOrder
            """)
    List<String> findTagNamesByPostId(@Param("postId") Long postId);

    @Query("select l.tagId from PostTagLinkEntity l where l.postId = :postId")
    List<Long> findTagIdsByPostId(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PostTagLinkEntity l where l.postId = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
