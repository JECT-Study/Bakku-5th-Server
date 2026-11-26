package com.ject.user.infrastructure.adapter;

import com.ject.exchange.application.contract.ConfirmedAppointmentCountQuery;
import com.ject.user.application.port.InProgressExchangePort;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 교환 수는 교환 BC의 확정 약속 수로 센다 (MB-5, G-09-2).
 */
@Component
public class InProgressExchangeAdapter implements InProgressExchangePort {

    private final ConfirmedAppointmentCountQuery confirmedAppointmentCountQuery;

    public InProgressExchangeAdapter(ConfirmedAppointmentCountQuery confirmedAppointmentCountQuery) {
        this.confirmedAppointmentCountQuery = confirmedAppointmentCountQuery;
    }

    @Override
    public int countInProgress(Long memberId) {
        return confirmedAppointmentCountQuery.countByMemberId(memberId);
    }
}
