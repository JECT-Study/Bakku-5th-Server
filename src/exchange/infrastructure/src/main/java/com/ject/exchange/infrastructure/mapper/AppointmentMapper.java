package com.ject.exchange.infrastructure.mapper;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.infrastructure.entity.AppointmentEntity;
import com.ject.exchange.infrastructure.entity.AppointmentItemEntity;

import java.util.List;
import java.util.stream.IntStream;

public class AppointmentMapper {

    public static AppointmentEntity toEntity(Appointment appointment) {
        return AppointmentEntity.builder()
                .id(appointment.getId())
                .exchangeId(appointment.getExchangeId())
                .proposerUserId(appointment.getProposerUserId())
                .receiverUserId(appointment.getReceiverUserId())
                .status(appointment.getStatus())
                .timeType(appointment.getTime().type())
                .scheduledAt(appointment.getTime().scheduledAt())
                .placeName(appointment.getPlace().name())
                .placeAddress(appointment.getPlace().address())
                .placeLat(appointment.getPlace().lat())
                .placeLng(appointment.getPlace().lng())
                .expiresAt(appointment.getExpiresAt())
                .proposerReminderOffsetMinutes(appointment.getProposerReminderOffsetMinutes())
                .receiverReminderOffsetMinutes(appointment.getReceiverReminderOffsetMinutes())
                .confirmedAt(appointment.getConfirmedAt())
                .build();
    }

    public static List<AppointmentItemEntity> toItemEntities(AppointmentEntity appointmentEntity, Appointment appointment) {
        List<Long> offeredItemIds = appointment.getItemIds().values();
        return IntStream.range(0, offeredItemIds.size())
                .mapToObj(i -> AppointmentItemEntity.builder()
                        .appointment(appointmentEntity)
                        .offeredItemId(offeredItemIds.get(i))
                        .sortOrder(i)
                        .build())
                .toList();
    }

    public static Appointment toDomain(AppointmentEntity entity, List<AppointmentItemEntity> items) {
        return Appointment.of(
                entity.getId(),
                entity.getExchangeId(),
                entity.getProposerUserId(),
                entity.getReceiverUserId(),
                entity.getStatus(),
                new AppointmentTime(entity.getTimeType(), entity.getScheduledAt()),
                new Place(entity.getPlaceName(), entity.getPlaceAddress(), entity.getPlaceLat(), entity.getPlaceLng()),
                new AppointmentItemIds(items.stream().map(AppointmentItemEntity::getOfferedItemId).toList()),
                entity.getExpiresAt(),
                entity.getProposerReminderOffsetMinutes(),
                entity.getReceiverReminderOffsetMinutes(),
                entity.getConfirmedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
