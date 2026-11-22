package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.user.domain.domain.ReportReason;
import com.ject.user.domain.domain.ReportTargetType;
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
@Table(name = "user_report")
public class ReportJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private ReportReason reason;

    @Column(updatable = false, length = 300)
    private String detail;

    @Column(name = "reported_member_id")
    private Long reportedMemberId;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    protected ReportJpaEntity() {
    }
}
