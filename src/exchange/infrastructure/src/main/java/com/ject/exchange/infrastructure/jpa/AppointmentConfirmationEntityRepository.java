package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.infrastructure.entity.AppointmentConfirmationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentConfirmationEntityRepository extends JpaRepository<AppointmentConfirmationEntity, Long> {

    List<AppointmentConfirmationEntity> findByAppointmentIdOrderByAnsweredAtAscIdAsc(Long appointmentId);

    List<AppointmentConfirmationEntity> findByAppointmentIdInOrderByAnsweredAtAscIdAsc(List<Long> appointmentIds);
}
