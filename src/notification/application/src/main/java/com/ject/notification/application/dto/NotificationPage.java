package com.ject.notification.application.dto;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;

import java.time.Instant;
import java.util.List;

/**
 * 알림 목록 한 페이지 (NT-1).
 *
 * @param nextCursorId 다음 페이지 커서(마지막 알림 id). 마지막 페이지면 null
 */
public record NotificationPage(List<NotificationItem> content, Long nextCursorId, boolean hasNext) {

    /**
     * @param targetType 이동할 대상이 없으면 null
     * @param targetId   이동할 대상이 없으면 null
     */
    public record NotificationItem(Long notificationId, NotificationType type, String title, String body,
                                   NotificationTargetType targetType, Long targetId, boolean read,
                                   Instant createdAt) {
    }
}
