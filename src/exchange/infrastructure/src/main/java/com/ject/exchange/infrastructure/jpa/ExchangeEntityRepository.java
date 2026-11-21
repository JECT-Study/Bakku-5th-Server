package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExchangeEntityRepository extends JpaRepository<ExchangeEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExchangeEntity e where e.id = :id")
    Optional<ExchangeEntity> findWithLockById(@Param("id") Long id);
}
