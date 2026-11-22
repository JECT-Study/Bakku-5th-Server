package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.BlockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BlockJpaRepository extends JpaRepository<BlockJpaEntity, Long> {

    /**
     * @return 추가한 행 수. 이미 차단했으면 0
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO user_block (blocker_id, blocked_id, blocked_at, created_at, updated_at)
            VALUES (:blockerId, :blockedId, :blockedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (blocker_id, blocked_id) DO NOTHING
            """)
    int insertIfAbsent(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId,
                       @Param("blockedAt") Instant blockedAt);

    Optional<BlockJpaEntity> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
