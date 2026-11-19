package com.ject.post.infrastructure.jpa;

import com.ject.post.infrastructure.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostEntityRepository extends JpaRepository<PostEntity, Long>, JpaSpecificationExecutor<PostEntity> {
}
