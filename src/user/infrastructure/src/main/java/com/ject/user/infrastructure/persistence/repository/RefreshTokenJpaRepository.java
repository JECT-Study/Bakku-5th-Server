package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByDeviceId(String deviceId);

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    Optional<RefreshTokenJpaEntity> findByPreviousTokenHash(String previousTokenHash);

    /**
     * 현재 토큰이 아직 회전 전 토큰일 때만 바꾼다. 동시에 온 재발급은 하나만 성공하고 나머지는 0행이다 (결정 C3 · C8).
     * 벌크 UPDATE라 수정 시각 자동 기록이 되지 않아 직접 넣는다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RefreshTokenJpaEntity t
               SET t.tokenHash = :tokenHash, t.previousTokenHash = :previousTokenHash,
                   t.rotatedAt = :rotatedAt, t.updatedAt = :rotatedAt
             WHERE t.id = :id AND t.tokenHash = :previousTokenHash
            """)
    int rotate(@Param("id") Long id, @Param("tokenHash") String tokenHash,
               @Param("previousTokenHash") String previousTokenHash, @Param("rotatedAt") Instant rotatedAt);
}
