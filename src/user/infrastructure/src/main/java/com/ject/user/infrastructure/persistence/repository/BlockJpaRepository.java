package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.infrastructure.persistence.entity.BlockJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
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

    /**
     * @return 지운 행 수. 차단하지 않은 회원이면 0
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM BlockJpaEntity b WHERE b.blockerId = :blockerId AND b.blockedId = :blockedId")
    int delete(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Query("""
            SELECT b FROM BlockJpaEntity b
            WHERE b.blockerId = :blockerId AND b.id < :cursorId
              AND EXISTS (SELECT 1 FROM MemberJpaEntity m WHERE m.id = b.blockedId AND m.status <> :excludedStatus)
            ORDER BY b.id DESC
            """)
    List<BlockJpaEntity> findPage(@Param("blockerId") Long blockerId, @Param("cursorId") Long cursorId,
                                  @Param("excludedStatus") MemberStatus excludedStatus, Limit limit);

    @Query("""
            SELECT CASE WHEN b.blockerId = :memberId THEN b.blockedId ELSE b.blockerId END
            FROM BlockJpaEntity b
            WHERE b.blockerId = :memberId OR b.blockedId = :memberId
            """)
    List<Long> findRelatedMemberIds(@Param("memberId") Long memberId);

    @Query("""
            SELECT COUNT(b) > 0 FROM BlockJpaEntity b
            WHERE (b.blockerId = :memberId AND b.blockedId = :otherMemberId)
               OR (b.blockerId = :otherMemberId AND b.blockedId = :memberId)
            """)
    boolean existsBetween(@Param("memberId") Long memberId, @Param("otherMemberId") Long otherMemberId);
}
