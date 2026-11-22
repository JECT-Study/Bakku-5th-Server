package com.ject.exchange.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "exchange_appointment_confirmation")
public class AppointmentConfirmationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer", nullable = false, length = 20)
    private ConfirmationAnswer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "not_met_reason", length = 20)
    private NotMetReason notMetReason;

    @Column(name = "not_met_detail", length = 100)
    private String notMetDetail;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected AppointmentConfirmationEntity() {}

    @Builder
    public AppointmentConfirmationEntity(Long id, AppointmentEntity appointment, Long userId,
                                         ConfirmationAnswer answer, NotMetReason notMetReason,
                                         String notMetDetail, Instant answeredAt) {
        this.id = id;
        this.appointment = appointment;
        this.userId = userId;
        this.answer = answer;
        this.notMetReason = notMetReason;
        this.notMetDetail = notMetDetail;
        this.answeredAt = answeredAt;
    }
}
