package com.ject.notification.infrastructure.persistence.mapper;

import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ject.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        notificationJpaRepository.saveAll(notifications.stream()
                .map(NotificationRepositoryAdapter::toEntity)
                .toList());
    }

    private static NotificationJpaEntity toEntity(Notification notification) {
        return new NotificationJpaEntity(notification.getReceiverId(), notification.getType(),
                notification.getTitle(), notification.getBody(), notification.getTargetType(),
                notification.getTargetId());
    }
}
