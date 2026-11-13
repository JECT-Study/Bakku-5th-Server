package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.MemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    // lower(nickname) 고유 인덱스를 타도록 칼럼 쪽도 lower로 비교한다
    @Query("select m from MemberJpaEntity m where lower(m.nickname) = lower(:nickname)")
    Optional<MemberJpaEntity> findByNicknameIgnoreCase(@Param("nickname") String nickname);

    @Modifying(clearAutomatically = true)
    @Query("update MemberJpaEntity m set m.completedCount = m.completedCount + 1, m.updatedAt = :now"
            + " where m.id in :ids")
    int increaseCompletedCount(@Param("ids") Collection<Long> ids, @Param("now") Instant now);
}
