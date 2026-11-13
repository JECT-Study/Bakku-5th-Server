package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.infrastructure.entity.OfferedItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferedItemEntityRepository extends JpaRepository<OfferedItemEntity, Long> {
    List<OfferedItemEntity> findByExchangeIdOrderBySortOrder(Long exchangeId);
}
