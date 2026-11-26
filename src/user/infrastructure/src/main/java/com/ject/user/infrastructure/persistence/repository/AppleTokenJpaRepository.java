package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.AppleTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppleTokenJpaRepository extends JpaRepository<AppleTokenJpaEntity, String> {
}
