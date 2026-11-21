package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.WithdrawalReasonJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalReasonJpaRepository extends JpaRepository<WithdrawalReasonJpaEntity, Long> {
}
