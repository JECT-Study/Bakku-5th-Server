package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.infrastructure.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

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
}
