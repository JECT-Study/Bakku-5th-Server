package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.ReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {

    /**
     * @return 새 신고 id. 이미 신고했으면 empty
     */
    @Query(nativeQuery = true, value = """
            INSERT INTO user_report (reporter_id, target_type, target_id, reason, detail, reported_member_id,
                                     reported_at, created_at, updated_at)
            VALUES (:reporterId, :targetType, :targetId, :reason, :detail, :reportedMemberId,
                    :reportedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (reporter_id, target_type, target_id) DO NOTHING
            RETURNING id
            """)
    Optional<Long> insertIfAbsent(@Param("reporterId") Long reporterId, @Param("targetType") String targetType,
                                  @Param("targetId") Long targetId, @Param("reason") String reason,
                                  @Param("detail") String detail, @Param("reportedMemberId") Long reportedMemberId,
                                  @Param("reportedAt") Instant reportedAt);
}
