package com.ject.notification.application.port;

import com.ject.notification.domain.domain.Notification;

import java.util.List;

/**
 * 알림 목록 저장소 출력 포트.
 */
public interface NotificationRepository {

    void saveAll(List<Notification> notifications);
}
