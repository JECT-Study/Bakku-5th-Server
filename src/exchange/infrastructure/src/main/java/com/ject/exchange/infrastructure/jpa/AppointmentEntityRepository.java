package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.infrastructure.entity.AppointmentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentEntityRepository extends JpaRepository<AppointmentEntity, Long> {

    List<AppointmentEntity> findByExchangeIdOrderById(Long exchangeId);

    @Query("""
            select count(a) > 0
            from AppointmentEntity a, ExchangeEntity e
            where a.exchangeId = e.id
              and e.postId = :postId
              and e.id <> :excludedExchangeId
              and a.status = :status
              and a.expiresAt > :now
            """)
    boolean existsByPostAndStatusAfter(@Param("postId") Long postId,
                                       @Param("excludedExchangeId") Long excludedExchangeId,
                                       @Param("status") AppointmentStatus status,
                                       @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AppointmentEntity a where a.id = :id")
    Optional<AppointmentEntity> findWithLockById(@Param("id") Long id);

    @Query("""
            select e.postId
            from AppointmentEntity a, ExchangeEntity e
            where a.id = :appointmentId
              and a.exchangeId = e.id
            """)
    Optional<Long> findExchangePostIdByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Query("""
            select o.sourcePostId
            from AppointmentItemEntity ai, OfferedItemEntity o
            where ai.appointment.id = :appointmentId
              and ai.offeredItemId = o.id
              and o.sourcePostId is not null
            order by ai.sortOrder
            """)
    List<Long> findChosenSourcePostIdsByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Query("""
            select distinct e.postId
            from AppointmentEntity a, ExchangeEntity e
            where a.exchangeId = e.id
              and e.id <> :excludedExchangeId
              and a.status = :status
              and e.postId in :postIds
            """)
    List<Long> findBoundExchangePostIds(@Param("postIds") Collection<Long> postIds,
                                        @Param("status") AppointmentStatus status,
                                        @Param("excludedExchangeId") Long excludedExchangeId);

    @Query("""
            select distinct o.sourcePostId
            from AppointmentEntity a, AppointmentItemEntity ai, OfferedItemEntity o
            where ai.appointment.id = a.id
              and ai.offeredItemId = o.id
              and a.exchangeId <> :excludedExchangeId
              and a.status = :status
              and o.sourcePostId in :postIds
            """)
    List<Long> findBoundChosenSourcePostIds(@Param("postIds") Collection<Long> postIds,
                                            @Param("status") AppointmentStatus status,
                                            @Param("excludedExchangeId") Long excludedExchangeId);

    @Query("""
            select distinct a.id
            from AppointmentEntity a, ExchangeEntity e
            where a.exchangeId = e.id
              and e.id <> :excludedExchangeId
              and a.status = :status
              and a.expiresAt > :now
              and e.postId in :postIds
            """)
    List<Long> findIdsByExchangePostAndStatusAfter(@Param("postIds") Collection<Long> postIds,
                                                   @Param("excludedExchangeId") Long excludedExchangeId,
                                                   @Param("status") AppointmentStatus status,
                                                   @Param("now") Instant now);

    @Query("""
            select distinct a.id
            from AppointmentEntity a, AppointmentItemEntity ai, OfferedItemEntity o
            where ai.appointment.id = a.id
              and ai.offeredItemId = o.id
              and a.exchangeId <> :excludedExchangeId
              and a.status = :status
              and a.expiresAt > :now
              and o.sourcePostId in :postIds
            """)
    List<Long> findIdsByChosenSourcePostAndStatusAfter(@Param("postIds") Collection<Long> postIds,
                                                       @Param("excludedExchangeId") Long excludedExchangeId,
                                                       @Param("status") AppointmentStatus status,
                                                       @Param("now") Instant now);
}
