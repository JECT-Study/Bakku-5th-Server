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
}
