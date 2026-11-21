package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.user.domain.domain.WithdrawalReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_withdrawal_reason")
public class WithdrawalReasonJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private WithdrawalReason reason;

    @Column(name = "withdrawn_at", nullable = false, updatable = false)
    private Instant withdrawnAt;

    protected WithdrawalReasonJpaEntity() {
    }

    public WithdrawalReasonJpaEntity(Long memberId, WithdrawalReason reason, Instant withdrawnAt) {
        this.memberId = memberId;
        this.reason = reason;
        this.withdrawnAt = withdrawnAt;
    }
}
