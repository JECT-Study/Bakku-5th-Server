package com.ject.notification.infrastructure.persistence.repository;

import com.ject.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    /**
     * (receiver_id, id) 인덱스로 id 커서 조회 (결정 C6).
     */
    @Query("""
            SELECT n FROM NotificationJpaEntity n
            WHERE n.receiverId = :receiverId AND n.id < :cursorId AND n.createdAt >= :createdFrom
            ORDER BY n.id DESC
            """)
    List<NotificationJpaEntity> findPage(@Param("receiverId") Long receiverId, @Param("cursorId") Long cursorId,
                                         @Param("createdFrom") Instant createdFrom, Limit limit);

    /**
     * 안 읽은 알림 부분 인덱스(read_at IS NULL)로 센다 (결정 C6).
     */
    @Query("""
            SELECT COUNT(n) FROM NotificationJpaEntity n
            WHERE n.receiverId = :receiverId AND n.readAt IS NULL AND n.createdAt >= :createdFrom
            """)
    long countUnread(@Param("receiverId") Long receiverId, @Param("createdFrom") Instant createdFrom);

    /**
     * 안 읽은 알림만 바꿔서 동시에 눌러도 읽은 시각이 한 번만 정해진다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE NotificationJpaEntity n SET n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.id = :id AND n.receiverId = :receiverId AND n.readAt IS NULL AND n.createdAt >= :createdFrom
            """)
    int markRead(@Param("id") Long id, @Param("receiverId") Long receiverId, @Param("readAt") Instant readAt,
                 @Param("createdFrom") Instant createdFrom);

    boolean existsByIdAndReceiverIdAndCreatedAtGreaterThanEqual(Long id, Long receiverId, Instant createdFrom);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE NotificationJpaEntity n SET n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.receiverId = :receiverId AND n.readAt IS NULL AND n.createdAt >= :createdFrom
            """)
    int markAllRead(@Param("receiverId") Long receiverId, @Param("readAt") Instant readAt,
                    @Param("createdFrom") Instant createdFrom);
}
