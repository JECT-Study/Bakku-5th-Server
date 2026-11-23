package com.ject.notification.infrastructure.persistence.entity;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 저장은 키 · 받는 사람 기준 교체가 필요해 네이티브 쿼리로 하고, 이 엔티티는 조회 · 삭제에만 쓴다.
 */
@Entity
@Table(name = "notification_scheduled")
public class ScheduledNotificationJpaEntity extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "schedule_key", length = 100)
    private String scheduleKey;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    /** 문구용 값 (JSON) */
    @Column(name = "params", nullable = false, columnDefinition = "TEXT")
    private String params;

    @Column(name = "fire_at", nullable = false)
    private Instant fireAt;

    protected ScheduledNotificationJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getScheduleKey() {
        return scheduleKey;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public NotificationType getType() {
        return type;
    }

    public Long getSenderId() {
        return senderId;
    }

    public NotificationTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getParams() {
        return params;
    }

    public Instant getFireAt() {
        return fireAt;
    }
}
