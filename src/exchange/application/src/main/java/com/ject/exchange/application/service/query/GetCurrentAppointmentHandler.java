package com.ject.exchange.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetCurrentAppointmentQuery;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetCurrentAppointmentHandler implements QueryHandler<GetCurrentAppointmentQuery, Optional<AppointmentResult>> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public Optional<AppointmentResult> handle(GetCurrentAppointmentQuery query) {
        Exchange exchange = exchangeRepository.findById(query.exchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        // 교환 상태는 보지 않는다. 나가기 · 완료 시 약속 상태를 바꾸는 건 그 기능의 몫이다 (A-2와 같은 기준)
        if (!exchange.isParticipant(query.viewerId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        Instant now = serviceTime.now();
        return Appointment.currentAmong(appointmentRepository.findAllByExchangeId(exchange.getId()), now)
                .map(appointment -> AppointmentResult.of(appointment, exchange, sourcePostsOf(appointment, exchange),
                        query.viewerId(),
                        appointmentRepository.existsPendingProposalOnPost(exchange.getPostId(), exchange.getId()),
                        now));
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
