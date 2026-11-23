package com.ject.notification.application;

import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

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

    @Override
    public long countUnread(Long receiverId, Instant createdFrom) {
        return store.stream()
                .filter(notification -> notification.getReceiverId().equals(receiverId))
                .filter(notification -> !notification.getCreatedAt().isBefore(createdFrom))
                .filter(notification -> !notification.isRead())
                .count();
    }

    @Override
    public boolean markRead(Long notificationId, Long receiverId, Instant readAt, Instant createdFrom) {
        return markReadWhere(notification -> notification.getId().equals(notificationId)
                && notification.getReceiverId().equals(receiverId), readAt, createdFrom) > 0;
    }

    @Override
    public boolean exists(Long notificationId, Long receiverId, Instant createdFrom) {
        return store.stream().anyMatch(notification -> notification.getId().equals(notificationId)
                && notification.getReceiverId().equals(receiverId)
                && !notification.getCreatedAt().isBefore(createdFrom));
    }

    private int markReadWhere(Predicate<Notification> condition, Instant readAt, Instant createdFrom) {
        int updated = 0;
        for (int i = 0; i < store.size(); i++) {
            Notification notification = store.get(i);
            if (condition.test(notification) && !notification.isRead()
                    && !notification.getCreatedAt().isBefore(createdFrom)) {
                store.set(i, Notification.restore(notification.getId(), notification.getReceiverId(),
                        notification.getType(), notification.getTitle(), notification.getBody(),
                        notification.getTargetType(), notification.getTargetId(), readAt,
                        notification.getCreatedAt()));
                updated++;
            }
        }
        return updated;
    }

    /** id · 받은 시각이 정해진 저장된 알림을 넣는다 */
    void add(Notification notification) {
        store.add(notification);
    }

    List<Notification> findAll() {
        return List.copyOf(store);
    }
}
