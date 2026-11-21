package com.ject.exchange.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import lombok.Builder;

public record GetAppointmentQuery(
        Long appointmentId,
        Long viewerId
) implements Query<AppointmentResult> {
    @Builder
    public GetAppointmentQuery {
    }
}
