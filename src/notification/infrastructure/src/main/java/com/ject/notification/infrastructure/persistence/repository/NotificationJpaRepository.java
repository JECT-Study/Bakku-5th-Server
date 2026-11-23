package com.ject.notification.infrastructure.persistence.repository;

import com.ject.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
