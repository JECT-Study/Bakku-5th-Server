package com.ject.exchange.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "exchange_appointment_item")
public class AppointmentItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;

    @Column(name = "offered_item_id", nullable = false)
    private Long offeredItemId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected AppointmentItemEntity() {}

    @Builder
    public AppointmentItemEntity(Long id, AppointmentEntity appointment, Long offeredItemId, int sortOrder) {
        this.id = id;
        this.appointment = appointment;
        this.offeredItemId = offeredItemId;
        this.sortOrder = sortOrder;
    }
}
