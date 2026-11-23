package com.ject.notification.infrastructure.persistence.mapper;

import com.ject.notification.application.port.ScheduledNotificationRepository;
import com.ject.notification.domain.domain.ScheduledNotification;
import com.ject.notification.infrastructure.persistence.entity.ScheduledNotificationJpaEntity;
import com.ject.notification.infrastructure.persistence.repository.ScheduledNotificationJpaRepository;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class ScheduledNotificationRepositoryAdapter implements ScheduledNotificationRepository {

    private static final TypeReference<Map<String, String>> PARAMS_TYPE = new TypeReference<>() {
    };

    private final ScheduledNotificationJpaRepository scheduledNotificationJpaRepository;
    private final JsonMapper jsonMapper;

    public ScheduledNotificationRepositoryAdapter(ScheduledNotificationJpaRepository scheduledNotificationJpaRepository,
                                                  JsonMapper jsonMapper) {
        this.scheduledNotificationJpaRepository = scheduledNotificationJpaRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void saveAll(List<ScheduledNotification> notifications) {
        notifications.forEach(notification -> scheduledNotificationJpaRepository.upsert(
                notification.getScheduleKey(), notification.getReceiverId(), notification.getType().name(),
                notification.getSenderId(),
                notification.getTargetType() == null ? null : notification.getTargetType().name(),
                notification.getTargetId(), jsonMapper.writeValueAsString(notification.getParams()),
                notification.getFireAt()));
    }

    @Override
    public void deleteByScheduleKey(String scheduleKey) {
        scheduledNotificationJpaRepository.deleteByScheduleKey(scheduleKey);
    }

    @Override
    public List<ScheduledNotification> findDueForUpdate(Instant now, int limit) {
        return scheduledNotificationJpaRepository.findDueForUpdate(now, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteAllById(Collection<Long> ids) {
        scheduledNotificationJpaRepository.deleteAllByIdInBatch(ids);
    }

    private ScheduledNotification toDomain(ScheduledNotificationJpaEntity entity) {
        return ScheduledNotification.restore(entity.getId(), entity.getScheduleKey(), entity.getReceiverId(),
                entity.getType(), entity.getSenderId(), entity.getTargetType(), entity.getTargetId(),
                jsonMapper.readValue(entity.getParams(), PARAMS_TYPE), entity.getFireAt());
    }
}
