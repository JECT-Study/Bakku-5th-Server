package com.ject.notification.application;

import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;

import java.util.ArrayList;
import java.util.List;

class FakeNotificationRepository implements NotificationRepository {

    private final List<Notification> store = new ArrayList<>();

    @Override
    public void saveAll(List<Notification> notifications) {
        store.addAll(notifications);
    }

    List<Notification> findAll() {
        return List.copyOf(store);
    }
}
