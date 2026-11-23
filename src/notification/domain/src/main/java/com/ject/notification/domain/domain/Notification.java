package com.ject.notification.domain.domain;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;

import java.time.Instant;
import java.util.Objects;

/**
 * 알림 목록에 남는 알림 (X-01-1). 채팅 메시지는 저장하지 않는다.
 * 제목 · 내용은 보낸 때의 닉네임 · 제품명으로 만들어 그대로 보관한다.
 */
public class Notification {

    private final Long id;
    private final Long receiverId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final NotificationTargetType targetType;
    private final Long targetId;
    private final Instant readAt;
    private final Instant createdAt;

    private Notification(Long id, Long receiverId, NotificationType type, String title, String body,
                         NotificationTargetType targetType, Long targetId, Instant readAt, Instant createdAt) {
        this.id = id;
        this.receiverId = Objects.requireNonNull(receiverId);
        this.type = Objects.requireNonNull(type);
        this.title = Objects.requireNonNull(title);
        this.body = Objects.requireNonNull(body);
        this.targetType = targetType;
        this.targetId = targetId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    /**
     * @param targetType 누르면 이동할 대상, 없으면 null
     * @param targetId   대상 ID, 없으면 null
     */
    public static Notification create(Long receiverId, NotificationType type, NotificationContent content,
                                      NotificationTargetType targetType, Long targetId) {
        return new Notification(null, receiverId, type, content.title(), content.body(), targetType, targetId,
                null, null);
    }

    public static Notification restore(Long id, Long receiverId, NotificationType type, String title, String body,
                                       NotificationTargetType targetType, Long targetId, Instant readAt,
                                       Instant createdAt) {
        return new Notification(id, receiverId, type, title, body, targetType, targetId, readAt, createdAt);
    }

    public boolean isRead() {
        return readAt != null;
    }

    /** 저장 전이면 null */
    public Long getId() {
        return id;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    /** 이동할 대상이 없으면 null */
    public NotificationTargetType getTargetType() {
        return targetType;
    }

    /** 이동할 대상이 없으면 null */
    public Long getTargetId() {
        return targetId;
    }

    /** 안 읽었으면 null */
    public Instant getReadAt() {
        return readAt;
    }

    /** 저장 전이면 null */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
