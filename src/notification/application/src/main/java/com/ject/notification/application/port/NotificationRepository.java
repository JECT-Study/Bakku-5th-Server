package com.ject.notification.application.port;

import com.ject.notification.domain.domain.Notification;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository {

    void saveAll(List<Notification> notifications);

    /**
     * 받은 사람의 알림을 최신순(id 내림차순)으로 가져온다.
     *
     * @param createdFrom 이 시각 이후에 받은 알림만
     * @param cursorId    이 id보다 작은 알림만, 첫 페이지면 null
     */
    List<Notification> findPage(Long receiverId, Instant createdFrom, Long cursorId, int limit);

    /**
     * @param createdFrom 이 시각 이후에 받은 알림만
     */
    long countUnread(Long receiverId, Instant createdFrom);

    /**
     * 안 읽은 알림 하나를 읽음으로 바꾼다.
     *
     * @return 바꿨으면 true, 없거나 이미 읽었으면 false
     */
    boolean markRead(Long notificationId, Long receiverId, Instant readAt, Instant createdFrom);

    boolean exists(Long notificationId, Long receiverId, Instant createdFrom);
}
