package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.MemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("update MemberJpaEntity m set m.completedCount = m.completedCount + 1, m.updatedAt = :now"
            + " where m.id in :ids")
    int increaseCompletedCount(@Param("ids") Collection<Long> ids, @Param("now") Instant now);
}
