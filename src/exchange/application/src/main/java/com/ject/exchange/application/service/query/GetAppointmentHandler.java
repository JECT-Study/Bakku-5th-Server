package com.ject.exchange.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAppointmentHandler implements QueryHandler<GetAppointmentQuery, AppointmentResult> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public AppointmentResult handle(GetAppointmentQuery query) {
        Appointment appointment = appointmentRepository.findById(query.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(query.viewerId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        Instant now = serviceTime.now();
        return AppointmentResult.of(appointment, exchange, sourcePostsOf(appointment, exchange), query.viewerId(),
                appointmentRepository.existsPendingProposalOnPost(exchange.getPostId(), exchange.getId(), now), now);
    }

    private Map<Long, PostInfo> sourcePostsOf(Appointment appointment, Exchange exchange) {
        List<Long> sourcePostIds = AppointmentResult.sourcePostIdsOf(appointment, exchange);
        if (sourcePostIds.isEmpty()) {
            return Map.of();
        }
        return postPort.findAllByIds(sourcePostIds).stream()
                .collect(Collectors.toMap(PostInfo::id, Function.identity(), (first, second) -> first));
    }
}
