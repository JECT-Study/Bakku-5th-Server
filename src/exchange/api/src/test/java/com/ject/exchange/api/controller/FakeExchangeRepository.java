package com.ject.exchange.api.controller;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.repository.ExchangeRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 컨트롤러 테스트용 가짜 교환 저장소. Fake 버스가 실제 Handler를 거치게 해 에러 코드와 응답 흐름을 함께 확인한다.
 */
class FakeExchangeRepository implements ExchangeRepository {

    private final Map<Long, Exchange> store = new HashMap<>();

    @Override
    public Exchange save(Exchange exchange) {
        if (exchange.getId() == null) {
            throw new IllegalStateException("이 테스트는 id가 있는 교환만 저장한다");
        }
        store.put(exchange.getId(), exchange);
        return exchange;
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** 잠금은 DB에서만 의미가 있으므로 Fake는 findById와 같다. */
    @Override
    public Optional<Exchange> findByIdForUpdate(Long id) {
        return findById(id);
    }
}
