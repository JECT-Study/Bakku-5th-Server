package com.ject.notification.infrastructure.persistence.entity;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.common.jpa.BaseEntity;
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
@Table(name = "notification_history")
public class NotificationJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", nullable = false, updatable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, updatable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, updatable = false, length = 300)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", updatable = false, length = 20)
    private NotificationTargetType targetType;

    @Column(name = "target_id", updatable = false)
    private Long targetId;

    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(Long receiverId, NotificationType type, String title, String body,
                                 NotificationTargetType targetType, Long targetId) {
        this.receiverId = receiverId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}
