package com.ject.notification.application.dto;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;

/**
 * 기기로 보낼 푸시 한 건. 누르면 이동할 대상을 같이 보낸다.
 *
 * @param targetType 이동할 대상, 없으면 null
 * @param targetId   대상 ID, 없으면 null
 */
public record PushMessage(NotificationType type, String title, String body, NotificationTargetType targetType,
                          Long targetId) {
}
