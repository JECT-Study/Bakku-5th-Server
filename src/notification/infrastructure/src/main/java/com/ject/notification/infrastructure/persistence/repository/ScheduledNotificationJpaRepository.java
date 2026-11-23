package com.ject.notification.infrastructure.persistence.repository;

import com.ject.notification.infrastructure.persistence.entity.ScheduledNotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduledNotificationJpaRepository extends JpaRepository<ScheduledNotificationJpaEntity, Long> {

    /**
     * 같은 키 · 받는 사람이 있으면 바꾸고 없으면 추가한다. 키가 null이면 항상 추가된다.
     * null일 수 있는 값은 타입을 알 수 없어 CAST로 지정한다.
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO notification_scheduled (schedule_key, receiver_id, type, sender_id, target_type, target_id,
                                                params, fire_at, created_at, updated_at)
            VALUES (CAST(:scheduleKey AS VARCHAR), :receiverId, :type, CAST(:senderId AS BIGINT),
                    CAST(:targetType AS VARCHAR), CAST(:targetId AS BIGINT), :params, :fireAt,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (schedule_key, receiver_id) DO UPDATE
               SET type = EXCLUDED.type, sender_id = EXCLUDED.sender_id, target_type = EXCLUDED.target_type,
                   target_id = EXCLUDED.target_id, params = EXCLUDED.params, fire_at = EXCLUDED.fire_at,
                   updated_at = CURRENT_TIMESTAMP
            """)
    void upsert(@Param("scheduleKey") String scheduleKey, @Param("receiverId") Long receiverId,
                @Param("type") String type, @Param("senderId") Long senderId,
                @Param("targetType") String targetType, @Param("targetId") Long targetId,
                @Param("params") String params, @Param("fireAt") Instant fireAt);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ScheduledNotificationJpaEntity s WHERE s.scheduleKey = :scheduleKey")
    void deleteByScheduleKey(@Param("scheduleKey") String scheduleKey);

    @Query(nativeQuery = true, value = """
            SELECT * FROM notification_scheduled
             WHERE fire_at <= :now
             ORDER BY fire_at, id
             LIMIT :limit
               FOR UPDATE SKIP LOCKED
            """)
    List<ScheduledNotificationJpaEntity> findDueForUpdate(@Param("now") Instant now, @Param("limit") int limit);
}
