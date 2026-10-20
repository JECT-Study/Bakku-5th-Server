package com.ject.example.infrastructure.persistence.repository;

import com.ject.example.infrastructure.persistence.entity.FooJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FooJpaRepository extends JpaRepository<FooJpaEntity, Long> {
}
