package com.ject.exchange.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "exchange_appointment")
public class AppointmentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "proposer_user_id", nullable = false)
    private Long proposerUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_type", nullable = false)
    private AppointmentTimeType timeType;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "place_address", length = 255)
    private String placeAddress;

    @Column(name = "place_lat", nullable = false)
    private Double placeLat;

    @Column(name = "place_lng", nullable = false)
    private Double placeLng;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "proposer_reminder_offset_minutes")
    private Integer proposerReminderOffsetMinutes;

    @Column(name = "receiver_reminder_offset_minutes")
    private Integer receiverReminderOffsetMinutes;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected AppointmentEntity() {}

    @Builder
    public AppointmentEntity(Long id, Long exchangeId, Long proposerUserId, Long receiverUserId,
                             AppointmentStatus status, AppointmentTimeType timeType, Instant scheduledAt,
                             String placeName, String placeAddress, Double placeLat, Double placeLng,
                             Instant expiresAt, Integer proposerReminderOffsetMinutes,
                             Integer receiverReminderOffsetMinutes, Instant confirmedAt, Instant resolvedAt) {
        this.id = id;
        this.exchangeId = exchangeId;
        this.proposerUserId = proposerUserId;
        this.receiverUserId = receiverUserId;
        this.status = status;
        this.timeType = timeType;
        this.scheduledAt = scheduledAt;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.placeLat = placeLat;
        this.placeLng = placeLng;
        this.expiresAt = expiresAt;
        this.proposerReminderOffsetMinutes = proposerReminderOffsetMinutes;
        this.receiverReminderOffsetMinutes = receiverReminderOffsetMinutes;
        this.confirmedAt = confirmedAt;
        this.resolvedAt = resolvedAt;
    }
}
