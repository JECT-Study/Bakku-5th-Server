package com.ject.notification.infrastructure.persistence.mapper;

import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ject.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Override
    public List<Notification> findPage(Long receiverId, Instant createdFrom, Long cursorId, int limit) {
        Long before = cursorId == null ? Long.MAX_VALUE : cursorId;
        return notificationJpaRepository.findPage(receiverId, before, createdFrom, Limit.of(limit)).stream()
                .map(NotificationRepositoryAdapter::toDomain)
                .toList();
    }

    private static NotificationJpaEntity toEntity(Notification notification) {
        return new NotificationJpaEntity(notification.getReceiverId(), notification.getType(),
                notification.getTitle(), notification.getBody(), notification.getTargetType(),
                notification.getTargetId());
    }

    private static Notification toDomain(NotificationJpaEntity entity) {
        return Notification.restore(entity.getId(), entity.getReceiverId(), entity.getType(), entity.getTitle(),
                entity.getBody(), entity.getTargetType(), entity.getTargetId(), entity.getReadAt(),
                entity.getCreatedAt());
    }
}
