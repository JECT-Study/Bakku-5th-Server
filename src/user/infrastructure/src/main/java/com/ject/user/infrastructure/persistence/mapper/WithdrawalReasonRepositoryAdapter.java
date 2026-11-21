package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.WithdrawalReasonRepository;
import com.ject.user.domain.domain.WithdrawalReason;
import com.ject.user.infrastructure.persistence.entity.WithdrawalReasonJpaEntity;
import com.ject.user.infrastructure.persistence.repository.WithdrawalReasonJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class WithdrawalReasonRepositoryAdapter implements WithdrawalReasonRepository {

    private final WithdrawalReasonJpaRepository withdrawalReasonJpaRepository;

    public WithdrawalReasonRepositoryAdapter(WithdrawalReasonJpaRepository withdrawalReasonJpaRepository) {
        this.withdrawalReasonJpaRepository = withdrawalReasonJpaRepository;
    }

    @Override
    public void save(Long memberId, WithdrawalReason reason, Instant withdrawnAt) {
        withdrawalReasonJpaRepository.save(new WithdrawalReasonJpaEntity(memberId, reason, withdrawnAt));
    }
}
