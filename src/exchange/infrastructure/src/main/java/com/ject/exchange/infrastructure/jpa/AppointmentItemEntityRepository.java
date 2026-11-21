package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.infrastructure.entity.AppointmentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentItemEntityRepository extends JpaRepository<AppointmentItemEntity, Long> {

    List<AppointmentItemEntity> findByAppointmentIdOrderBySortOrder(Long appointmentId);

    List<AppointmentItemEntity> findByAppointmentIdInOrderBySortOrder(List<Long> appointmentIds);
}
