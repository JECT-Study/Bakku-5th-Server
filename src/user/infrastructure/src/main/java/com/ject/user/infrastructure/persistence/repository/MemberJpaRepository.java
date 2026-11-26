package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.SocialProvider;
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

    @Query("select m from MemberJpaEntity m where m.provider = :provider and m.socialIdHash = :socialIdHash"
            + " and m.status <> com.ject.user.domain.domain.MemberStatus.WITHDRAWN")
    Optional<MemberJpaEntity> findActiveBySocialIdHash(@Param("provider") SocialProvider provider,
                                                       @Param("socialIdHash") String socialIdHash);

    Optional<MemberJpaEntity> findFirstByProviderAndSocialIdHashAndStatusOrderByWithdrawnAtDesc(
            SocialProvider provider, String socialIdHash, MemberStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update MemberJpaEntity m set m.completedCount = m.completedCount + 1,"
            + " m.mannerScore = m.mannerScore + :mannerPoints, m.updatedAt = :now"
            + " where m.id in :ids")
    int increaseCompletedCount(@Param("ids") Collection<Long> ids, @Param("mannerPoints") int mannerPoints,
                               @Param("now") Instant now);

    /**
     * 마지막 닉네임 변경이 changedBefore 이전(또는 없음)일 때만 바꾼다 (결정 C8).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update MemberJpaEntity m set m.nickname = :nickname, m.nicknameChangedAt = :changedAt,"
            + " m.updatedAt = :changedAt"
            + " where m.id = :id and (m.nicknameChangedAt is null or m.nicknameChangedAt <= :changedBefore)")
    int changeNicknameIfChangeable(@Param("id") Long id, @Param("nickname") String nickname,
                                   @Param("changedAt") Instant changedAt,
                                   @Param("changedBefore") Instant changedBefore);
}
