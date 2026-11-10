package com.ject.exchange.infrastructure.repository;

import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeEntityRepository extends JpaRepository<ExchangeEntity, Long> {
}
