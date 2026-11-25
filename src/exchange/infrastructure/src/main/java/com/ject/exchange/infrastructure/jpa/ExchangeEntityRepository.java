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

    // 엔티티를 영속성 컨텍스트에 올리지 않도록 값만 읽는다. 그래야 잠금 뒤 교환을 처음 읽을 때 최신 행이 보인다
    @Query("select e.postId from ExchangeEntity e where e.id = :id")
    Optional<Long> findPostIdById(@Param("id") Long id);
}
