package com.ject.exchange.infrastructure.mapper;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.ConfirmationReply;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.infrastructure.entity.AppointmentConfirmationEntity;
import com.ject.exchange.infrastructure.entity.AppointmentEntity;
import com.ject.exchange.infrastructure.entity.AppointmentItemEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
                .resolvedAt(appointment.getResolvedAt())
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

    /**
     * 아직 저장되지 않은 응답만 행으로 만든다. 응답은 추가만 되고 바뀌지 않는다 (1인 1회)
     */
    public static List<AppointmentConfirmationEntity> toNewConfirmationEntities(
            AppointmentEntity appointmentEntity, Appointment appointment, List<AppointmentConfirmationEntity> stored) {
        Set<Long> storedUserIds = stored.stream()
                .map(AppointmentConfirmationEntity::getUserId)
                .collect(Collectors.toSet());
        return appointment.getConfirmationReplies().stream()
                .filter(reply -> !storedUserIds.contains(reply.userId()))
                .map(reply -> AppointmentConfirmationEntity.builder()
                        .appointment(appointmentEntity)
                        .userId(reply.userId())
                        .answer(reply.answer())
                        .notMetReason(reply.notMetReason())
                        .notMetDetail(reply.notMetDetail())
                        .answeredAt(reply.answeredAt())
                        .build())
                .toList();
    }

    public static Appointment toDomain(AppointmentEntity entity, List<AppointmentItemEntity> items,
                                       List<AppointmentConfirmationEntity> confirmations) {
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
                entity.getResolvedAt(),
                confirmations.stream()
                        .map(confirmation -> new ConfirmationReply(confirmation.getUserId(), confirmation.getAnswer(),
                                confirmation.getNotMetReason(), confirmation.getNotMetDetail(),
                                confirmation.getAnsweredAt()))
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
