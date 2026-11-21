package com.ject.exchange.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import lombok.Builder;

import java.util.Optional;

/**
 * 교환(채팅방)의 현재 유효한 약속 조회. 유효한 약속이 없으면 빈 Optional
 */
public record GetCurrentAppointmentQuery(
        Long exchangeId,
        Long viewerId
) implements Query<Optional<AppointmentResult>> {
    @Builder
    public GetCurrentAppointmentQuery {
    }
}
