package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.Place;

import java.time.Instant;
import java.util.List;

public record AppointmentResult(
        Long id,
        Long exchangeId,
        Long proposerUserId,
        Long receiverUserId,
        AppointmentStatus status,
        AppointmentTimeType timeType,
        Instant scheduledAt,
        Place place,
        List<AppointmentItemResult> items,
        Instant expiresAt,                      // PROPOSED가 아니면 null
        Integer myReminderOffsetMinutes,
        boolean otherActiveProposalsOnPost,
        Instant confirmedAt,
        Instant createdAt
) {}
