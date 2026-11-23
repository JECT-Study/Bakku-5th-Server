package com.ject.exchange.application.service.contract;

import com.ject.exchange.application.contract.ExchangeParticipantLookup;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeParticipantLookupService implements ExchangeParticipantLookup {

    private final ExchangeRepository exchangeRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isParticipant(Long exchangeId, Long memberId) {
        if (exchangeId == null || memberId == null) {
            return false;
        }
        return exchangeRepository.findById(exchangeId)
                .map(exchange -> exchange.isParticipant(memberId))
                .orElse(false);
    }
}
