package com.ject.exchange.domain.repository;

import com.ject.exchange.domain.domain.exchange.Exchange;

import java.util.Optional;

public interface ExchangeRepository {
    Exchange save(Exchange exchange);
    Optional<Exchange> findById(Long id);
}
