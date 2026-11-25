package com.ject.exchange.application.service.contract;

import com.ject.exchange.application.contract.ConfirmedAppointmentCountQuery;
import com.ject.exchange.domain.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmedAppointmentCountQueryService implements ConfirmedAppointmentCountQuery {

    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public int countByMemberId(Long memberId) {
        if (memberId == null) {
            return 0;
        }
        return appointmentRepository.countConfirmedByMemberId(memberId);
    }
}
