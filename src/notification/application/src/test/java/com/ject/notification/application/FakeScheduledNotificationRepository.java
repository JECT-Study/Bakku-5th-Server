package com.ject.notification.application;

import com.ject.notification.application.port.ScheduledNotificationRepository;
import com.ject.notification.domain.domain.ScheduledNotification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

class FakeScheduledNotificationRepository implements ScheduledNotificationRepository {

    private final List<ScheduledNotification> store = new ArrayList<>();
    private long sequence = 0;

    @Override
    public void saveAll(List<ScheduledNotification> notifications) {
        notifications.forEach(notification -> {
            if (notification.getScheduleKey() != null) {
                store.removeIf(saved -> notification.getScheduleKey().equals(saved.getScheduleKey())
                        && notification.getReceiverId().equals(saved.getReceiverId()));
            }
            store.add(ScheduledNotification.restore(++sequence, notification.getScheduleKey(),
                    notification.getReceiverId(), notification.getType(), notification.getSenderId(),
                    notification.getTargetType(), notification.getTargetId(), notification.getParams(),
                    notification.getFireAt()));
        });
    }

    @Override
    public void deleteByScheduleKey(String scheduleKey) {
        store.removeIf(saved -> Objects.equals(scheduleKey, saved.getScheduleKey()));
    }

    @Override
    public List<ScheduledNotification> findDueForUpdate(Instant now, int limit) {
        return store.stream()
                .filter(saved -> !saved.getFireAt().isAfter(now))
                .sorted(Comparator.comparing(ScheduledNotification::getFireAt)
                        .thenComparing(ScheduledNotification::getId))
                .limit(limit)
                .toList();
    }

    @Override
    public void deleteAllById(Collection<Long> ids) {
        store.removeIf(saved -> ids.contains(saved.getId()));
    }

    List<ScheduledNotification> findAll() {
        return List.copyOf(store);
    }
}
