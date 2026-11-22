package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_block")
public class BlockJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false, updatable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false, updatable = false)
    private Long blockedId;

    @Column(name = "blocked_at", nullable = false, updatable = false)
    private Instant blockedAt;

    protected BlockJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getBlockerId() {
        return blockerId;
    }

    public Long getBlockedId() {
        return blockedId;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }
}
