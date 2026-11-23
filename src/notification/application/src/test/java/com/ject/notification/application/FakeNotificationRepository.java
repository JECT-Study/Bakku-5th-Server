package com.ject.notification.application;

import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class FakeNotificationRepository implements NotificationRepository {

    private final List<Notification> store = new ArrayList<>();

    @Override
    public void saveAll(List<Notification> notifications) {
        store.addAll(notifications);
    }

    @Override
    public List<Notification> findPage(Long receiverId, Instant createdFrom, Long cursorId, int limit) {
        return store.stream()
                .filter(notification -> notification.getReceiverId().equals(receiverId))
                .filter(notification -> !notification.getCreatedAt().isBefore(createdFrom))
                .filter(notification -> cursorId == null || notification.getId() < cursorId)
                .sorted(Comparator.comparing(Notification::getId).reversed())
                .limit(limit)
                .toList();
    }

    /** id · 받은 시각이 정해진 저장된 알림을 넣는다 */
    void add(Notification notification) {
        store.add(notification);
    }

    List<Notification> findAll() {
        return List.copyOf(store);
    }
}
